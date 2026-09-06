package com.aque.asset;

import com.aque.asset.dto.request.AssetRequest;
import com.aque.asset.dto.response.AssetImportError;
import com.aque.asset.dto.response.AssetImportResponse;
import com.aque.asset.dto.response.AssetResponse;
import com.aque.asset.dto.response.NetWorthResponse;
import com.aque.exception.BusinessException;
import com.aque.person.Person;
import com.aque.person.PersonRepository;
import lombok.RequiredArgsConstructor;
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
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AssetService {

    private static final String COLUMN_PRODUTO = "Produto";
    private static final String COLUMN_VALOR_ATUALIZADO = "Valor Atualizado";
    private static final List<String> RENDA_FIXA_VALUE_COLUMNS = List.of(
            "Valor Atualizado CURVA", "Valor Atualizado FECHAMENTO", "Valor Atualizado MTM");
    private static final DataFormatter CELL_FORMATTER = new DataFormatter();

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

        List<AssetResponse> created = new ArrayList<>();
        List<AssetResponse> updated = new ArrayList<>();
        List<AssetImportError> errors = new ArrayList<>();

        try (XSSFWorkbook workbook = new XSSFWorkbook(file.getInputStream())) {
            for (Sheet sheet : workbook) {
                AssetType type = resolveAssetType(sheet.getSheetName());
                if (type == null) {
                    errors.add(new AssetImportError(sheet.getSheetName(), 1, "Aba não reconhecida", false));
                    continue;
                }

                importSheet(sheet, type, isRendaFixaVariantSheet(sheet.getSheetName()), created, updated, errors);
            }
        } catch (IOException | RuntimeException e) {
            throw new BusinessException("Arquivo não é um .xlsx de Posição da B3 válido", HttpStatus.BAD_REQUEST);
        }

        return new AssetImportResponse(created, updated, errors);
    }

    private void importSheet(Sheet sheet, AssetType type, boolean rendaFixaVariantColumns, List<AssetResponse> created, List<AssetResponse> updated, List<AssetImportError> errors) {
        Row header = sheet.getRow(0);
        Map<String, Integer> columns = headerIndex(header);
        Integer produtoColumn = columns.get(COLUMN_PRODUTO);
        if (produtoColumn == null) {
            errors.add(new AssetImportError(sheet.getSheetName(), 1, "Coluna 'Produto' não encontrada", false));
            return;
        }

        List<String> valueColumns = rendaFixaVariantColumns
                ? RENDA_FIXA_VALUE_COLUMNS
                : List.of(COLUMN_VALOR_ATUALIZADO);

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
                errors.add(new AssetImportError(sheet.getSheetName(), rowIndex + 1, "Produto vazio (possível linha de total/rodapé)", true));
                continue;
            }

            BigDecimal currentValue = resolveCurrentValue(row, columns, valueColumns);

            if (currentValue == null) {
                errors.add(new AssetImportError(sheet.getSheetName(), rowIndex + 1, "Valor atualizado indisponível", false));
                continue;
            }

            if (currentValue.signum() < 0) {
                errors.add(new AssetImportError(sheet.getSheetName(), rowIndex + 1, "Valor atualizado negativo", false));
                continue;
            }

            List<Asset> existing = assetRepository.findByNameIgnoreCaseAndType(sanitizedName, type);
            if (!existing.isEmpty()) {
                Asset asset = existing.get(0);
                asset.setCurrentValue(currentValue);
                updated.add(AssetResponse.from(assetRepository.save(asset)));
                continue;
            }

            Asset asset = new Asset();
            asset.setName(sanitizedName);
            asset.setType(type);
            asset.setCurrentValue(currentValue);
            created.add(AssetResponse.from(assetRepository.save(asset)));
        }
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

    private BigDecimal cellAsNumeric(Cell cell) {
        if (cell == null || cell.getCellType() != CellType.NUMERIC) {
            return null;
        }

        return BigDecimal.valueOf(cell.getNumericCellValue());
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
}
