package com.aque.asset;

import com.aque.asset.dto.request.AssetRequest;
import com.aque.asset.dto.response.AssetImportError;
import com.aque.asset.dto.response.AssetImportResponse;
import com.aque.asset.dto.response.AssetImportSheetSummary;
import com.aque.asset.dto.response.AssetResponse;
import com.aque.asset.dto.response.NetWorthResponse;
import com.aque.exception.BusinessException;
import com.aque.person.Person;
import com.aque.person.PersonRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class AssetService {

    private static final String COLUMN_PRODUTO = "Produto";
    private static final String COLUMN_VALOR_ATUALIZADO = "Valor Atualizado";
    private static final List<String> RENDA_FIXA_VALUE_COLUMNS = List.of(
            "Valor Atualizado CURVA", "Valor Atualizado FECHAMENTO", "Valor Atualizado MTM");
    private static final DataFormatter CELL_FORMATTER = new DataFormatter();
    private static final int MONEY_SCALE = 2;

    private final AssetRepository assetRepository;
    private final PersonRepository personRepository;

    public List<AssetResponse> findAll(UUID personId) {
        List<Asset> assets = personId != null
                ? assetRepository.findByPersonId(personId)
                : assetRepository.findAll();

        return assets.stream()
                .map(AssetResponse::from)
                .toList();
    }

    public AssetResponse create(AssetRequest request) {
        Asset asset = new Asset();
        asset.setName(request.name());
        asset.setType(request.type());
        asset.setCurrentValue(request.currentValue());
        asset.setPerson(resolvePerson(request.personId()));
        return AssetResponse.from(assetRepository.save(asset));
    }

    public AssetResponse update(UUID id, AssetRequest request) {
        Asset asset = findById(id);

        asset.setName(request.name());
        asset.setType(request.type());
        asset.setCurrentValue(request.currentValue());
        asset.setPerson(resolvePerson(request.personId()));

        return AssetResponse.from(assetRepository.save(asset));
    }

    public void delete(UUID id) {
        assetRepository.delete(findById(id));
    }

    public NetWorthResponse getNetWorth() {
        return new NetWorthResponse(assetRepository.sumCurrentValue());
    }

    public AssetImportResponse importFromXlsx(MultipartFile file) {
        if (file.isEmpty()) {
            throw new BusinessException("Arquivo vazio", HttpStatus.BAD_REQUEST);
        }

        ImportTally tally = new ImportTally();

        try (XSSFWorkbook workbook = new XSSFWorkbook(file.getInputStream())) {
            for (Sheet sheet : workbook) {
                AssetType type = resolveAssetType(sheet.getSheetName());
                if (type == null) {
                    tally.errors.add(new AssetImportError(sheet.getSheetName(), 1, "Aba não reconhecida", false));
                    continue;
                }

                importSheet(sheet, type, tally);
            }
        } catch (IOException | RuntimeException e) {
            throw new BusinessException("Arquivo não é um .xlsx de Posição da B3 válido", HttpStatus.BAD_REQUEST);
        }

        return new AssetImportResponse(
                tally.created,
                tally.updated,
                findMissing(tally),
                tally.errors,
                tally.sheets,
                sumOf(tally.sheets, AssetImportSheetSummary::totalRead),
                sumOf(tally.sheets, AssetImportSheetSummary::totalPersisted)
        );
    }

    private void importSheet(Sheet sheet, AssetType type, ImportTally tally) {
        Row header = sheet.getRow(0);
        Map<String, Integer> columns = headerIndex(header);
        Integer produtoColumn = columns.get(COLUMN_PRODUTO);
        if (produtoColumn == null) {
            tally.errors.add(new AssetImportError(sheet.getSheetName(), 1, "Coluna 'Produto' não encontrada", false));
            return;
        }

        List<String> valueColumns = isRendaFixaVariantSheet(sheet.getSheetName())
                ? RENDA_FIXA_VALUE_COLUMNS
                : List.of(COLUMN_VALOR_ATUALIZADO);
        Integer codeColumn = columns.get(resolveCodeColumn(sheet.getSheetName()));
        if (codeColumn == null) {
            // sem os códigos dessa aba não dá pra saber o que está ausente. As linhas ainda
            // importam pelo fallback por nome, então created/updated ficam cheios e a guarda
            // de "nada importado" do findMissing não protege — quem decide o que fazer com
            // isso é o findMissing, que sabe se havia algo a reportar
            tally.sheetsWithoutCodeColumn.add(sheet.getSheetName());
        }

        BigDecimal totalRead = BigDecimal.ZERO;
        int rows = 0;
        // chaveado pelo id da linha no banco: se duas linhas colapsarem no mesmo ativo, a
        // segunda sobrescreve a primeira aqui igual acontece no banco, e o total
        // persistido acusa a diferença. Chavear pela identidade do objeto funcionaria só
        // enquanto um mesmo EntityManager atender o import inteiro (open-in-view ligado) —
        // sem isso, `save` pode devolver instâncias diferentes pra mesma linha e o colapso
        // passaria como se estivesse tudo certo
        Map<UUID, BigDecimal> persisted = new LinkedHashMap<>();

        for (int rowIndex = 1; rowIndex <= sheet.getLastRowNum(); rowIndex++) {
            Row row = sheet.getRow(rowIndex);
            if (row == null) {
                continue;
            }

            String name = cellAsString(row.getCell(produtoColumn));
            String sanitizedName = name == null ? "" : name.trim();

            // O export de "Posição" da B3 sempre inclui linhas de rodapé/subtotal no final
            // de cada aba com a coluna Produto vazia — sem essa checagem viram "ativos
            // fantasma" sem nome (o valor delas costuma ser só o total da aba).
            if (sanitizedName.isEmpty()) {
                tally.errors.add(new AssetImportError(sheet.getSheetName(), rowIndex + 1, "Produto vazio (possível linha de total/rodapé)", true));
                continue;
            }

            // registrado antes das validações de valor de propósito: seenCodes responde
            // "esse código apareceu no arquivo", não "essa linha foi importada com
            // sucesso". Se uma linha ilegível não registrasse o código, o ativo dela
            // entraria em `missing` — e `missing` é a lista que o usuário usa pra decidir
            // o que apagar
            String externalCode = codeColumn == null ? null : blankToNull(cellAsString(row.getCell(codeColumn)));
            if (externalCode != null) {
                tally.seenCodes.add(externalCode);
            }

            BigDecimal currentValue = resolveCurrentValue(row, columns, valueColumns);

            if (currentValue == null) {
                tally.errors.add(new AssetImportError(sheet.getSheetName(), rowIndex + 1, "Valor atualizado indisponível", false));
                continue;
            }

            if (currentValue.signum() < 0) {
                tally.errors.add(new AssetImportError(sheet.getSheetName(), rowIndex + 1, "Valor atualizado negativo", false));
                continue;
            }

            totalRead = totalRead.add(currentValue);
            rows++;

            Asset existing = findExisting(externalCode, sanitizedName, type);
            if (existing != null) {
                existing.setName(sanitizedName);
                existing.setCurrentValue(currentValue);
                if (externalCode != null) {
                    existing.setExternalCode(externalCode);
                }
                Asset saved = assetRepository.save(existing);
                persisted.put(saved.getId(), currentValue);
                tally.updated.add(AssetResponse.from(saved));
                continue;
            }

            Asset asset = new Asset();
            asset.setName(sanitizedName);
            asset.setType(type);
            asset.setCurrentValue(currentValue);
            asset.setExternalCode(externalCode);
            Asset saved = assetRepository.save(asset);
            persisted.put(saved.getId(), currentValue);
            tally.created.add(AssetResponse.from(saved));
        }

        BigDecimal totalPersisted = persisted.values().stream()
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // divergir aqui significa que linhas distintas colapsaram no mesmo ativo — o bug da
        // #37. Só devolver os dois números num campo da resposta repetiria o erro que
        // deixou esse bug passar meses: vai pro log e pra `errors`, que é o que a tela mostra
        if (totalPersisted.compareTo(totalRead) != 0) {
            log.warn("Reconciliação divergente na aba {}: lido {}, persistido {}",
                    sheet.getSheetName(), totalRead, totalPersisted);
            tally.errors.add(new AssetImportError(
                    sheet.getSheetName(), 0,
                    "Total persistido (%s) diverge do total lido (%s) — linhas distintas com o mesmo código"
                            .formatted(totalPersisted, totalRead),
                    false));
        }

        tally.sheets.add(new AssetImportSheetSummary(sheet.getSheetName(), rows, totalRead, totalPersisted));
    }

    /**
     * Busca em duas etapas. O código é a chave de verdade; o fallback por nome existe só pra
     * adotar ativos importados antes da V9, que ainda não têm código — e por isso é
     * restrito a {@code externalCode IS NULL}. Sem essa restrição, a segunda linha de um
     * mesmo produto roubaria o ativo que a primeira acabou de adotar, que é exatamente o
     * bug da #37.
     */
    private Asset findExisting(String externalCode, String name, AssetType type) {
        if (externalCode != null) {
            List<Asset> byCode = assetRepository.findByExternalCode(externalCode);
            if (!byCode.isEmpty()) {
                return byCode.getFirst();
            }
        }

        List<Asset> byName = assetRepository.findByNameIgnoreCaseAndTypeAndExternalCodeIsNull(name, type);
        return byName.isEmpty() ? null : byName.getFirst();
    }

    /**
     * Ativos que vieram de um import anterior e não estão no arquivo: vendidos, vencidos, ou
     * o usuário subiu um extrato parcial. Nunca apaga — um arquivo errado não pode custar
     * registros, e um ativo importado com pessoa atribuída à mão perderia a atribuição.
     * Ativo manual (sem código) nunca entra: ele nunca veio de extrato nenhum.
     */
    private List<AssetResponse> findMissing(ImportTally tally) {
        if (tally.created.isEmpty() && tally.updated.isEmpty()) {
            return List.of();
        }

        List<Asset> importedBefore = assetRepository.findByExternalCodeIsNotNull();

        // Uma aba sem coluna de código não registra código nenhum, então todo ativo com
        // código pareceria ausente. Suprimir a lista é a direção segura — mas só é digno de
        // erro quando havia de fato algo a verificar, senão toda planilha sem código (um
        // export antigo, uma fixture de teste) viraria ruído acionável por ninguém.
        if (!tally.sheetsWithoutCodeColumn.isEmpty()) {
            if (!importedBefore.isEmpty()) {
                tally.sheetsWithoutCodeColumn.forEach(sheet -> tally.errors.add(new AssetImportError(
                        sheet, 1,
                        "Coluna de código não encontrada — ativos ausentes do arquivo não foram verificados",
                        false)));
            }
            return List.of();
        }

        return importedBefore.stream()
                .filter(asset -> !tally.seenCodes.contains(asset.getExternalCode()))
                .map(AssetResponse::from)
                .toList();
    }

    private BigDecimal resolveCurrentValue(Row row, Map<String, Integer> columns, List<String> valueColumns) {
        for (String columnName : valueColumns) {
            Integer columnIndex = columns.get(columnName);
            if (columnIndex == null) {
                continue;
            }

            BigDecimal value = cellAsNumeric(row.getCell(columnIndex));
            if (value != null) {
                return value;
            }
        }

        return null;
    }

    private Map<String, Integer> headerIndex(Row header) {
        Map<String, Integer> columns = new HashMap<>();
        if (header == null) {
            return columns;
        }

        for (Cell cell : header) {
            columns.put(cellAsString(cell), cell.getColumnIndex());
        }

        return columns;
    }

    private String cellAsString(Cell cell) {
        return cell == null ? null : CELL_FORMATTER.formatCellValue(cell);
    }

    private String blankToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    // arredonda na leitura, e não só na gravação: o double do POI carrega ruído binário, e
    // sem isso o valor comparado na reconciliação não seria o mesmo que o Postgres guarda
    // em NUMERIC(15,2)
    private BigDecimal cellAsNumeric(Cell cell) {
        if (cell == null || cell.getCellType() != CellType.NUMERIC) {
            return null;
        }

        return BigDecimal.valueOf(cell.getNumericCellValue()).setScale(MONEY_SCALE, RoundingMode.HALF_UP);
    }

    private AssetType resolveAssetType(String sheetName) {
        return switch (normalizeSheetName(sheetName)) {
            case "acoes" -> AssetType.ACAO;
            case "fundo de investimento" -> AssetType.FUNDO;
            case "renda fixa" -> AssetType.RENDA_FIXA;
            case "tesouro direto" -> AssetType.RENDA_FIXA;
            default -> null;
        };
    }

    // cada aba do extrato nomeia o identificador da posição de um jeito diferente
    private String resolveCodeColumn(String sheetName) {
        return switch (normalizeSheetName(sheetName)) {
            case "acoes", "fundo de investimento" -> "Código de Negociação";
            case "renda fixa" -> "Código";
            case "tesouro direto" -> "Código ISIN";
            default -> null;
        };
    }

    // Só a aba "Renda Fixa" tem as 3 variantes de coluna de valor (MTM/CURVA/FECHAMENTO);
    // "Tesouro Direto" também vira AssetType.RENDA_FIXA (é renda fixa na prática), mas sua
    // planilha usa a coluna "Valor Atualizado" simples, igual Ações/Fundo — por isso essa
    // checagem é por nome de aba, nunca pelo AssetType já resolvido.
    private boolean isRendaFixaVariantSheet(String sheetName) {
        return "renda fixa".equals(normalizeSheetName(sheetName));
    }

    private String normalizeSheetName(String sheetName) {
        return Normalizer.normalize(sheetName, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .trim()
                .toLowerCase();
    }

    private BigDecimal sumOf(List<AssetImportSheetSummary> sheets,
                             java.util.function.Function<AssetImportSheetSummary, BigDecimal> field) {
        return sheets.stream().map(field).reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private Asset findById(UUID id) {
        return assetRepository.findById(id)
                .orElseThrow(() -> new BusinessException(
                        "Ativo não encontrado",
                        HttpStatus.NOT_FOUND
                ));
    }

    private Person resolvePerson(UUID personId) {
        if (personId == null) {
            return null;
        }

        return personRepository.findById(personId)
                .orElseThrow(() -> new BusinessException(
                        "Pessoa não encontrada",
                        HttpStatus.NOT_FOUND
                ));
    }

    // acumulador do import: substitui a fileira de listas que era passada aba a aba
    private static final class ImportTally {
        private final List<AssetResponse> created = new ArrayList<>();
        private final List<AssetResponse> updated = new ArrayList<>();
        private final List<AssetImportError> errors = new ArrayList<>();
        private final List<AssetImportSheetSummary> sheets = new ArrayList<>();
        private final Set<String> seenCodes = new HashSet<>();
        private final List<String> sheetsWithoutCodeColumn = new ArrayList<>();
    }
}
