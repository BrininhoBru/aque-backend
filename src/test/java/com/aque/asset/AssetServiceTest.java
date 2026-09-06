package com.aque.asset;

import com.aque.asset.dto.request.AssetRequest;
import com.aque.asset.dto.response.AssetImportResponse;
import com.aque.asset.dto.response.AssetResponse;
import com.aque.asset.dto.response.NetWorthResponse;
import com.aque.exception.BusinessException;
import com.aque.person.Person;
import com.aque.person.PersonRepository;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AssetServiceTest {

    @Mock
    private AssetRepository assetRepository;

    @Mock
    private PersonRepository personRepository;

    @InjectMocks
    private AssetService service;

    private Asset vale3;
    private Person bruno;

    @BeforeEach
    void setup() {
        bruno = new Person();
        bruno.setId(UUID.randomUUID());
        bruno.setName("Bruno");

        vale3 = new Asset();
        vale3.setId(UUID.randomUUID());
        vale3.setName("VALE3");
        vale3.setType(AssetType.ACAO);
        vale3.setCurrentValue(new BigDecimal("314.48"));
    }

    @Test
    void findAll_semFiltro_retornaTodos() {
        when(assetRepository.findAll()).thenReturn(List.of(vale3));

        List<AssetResponse> result = service.findAll(null);

        assertThat(result).hasSize(1);
        verify(assetRepository).findAll();
    }

    @Test
    void findAll_comPersonId_usaFindByPersonId() {
        when(assetRepository.findByPersonId(bruno.getId())).thenReturn(List.of(vale3));

        List<AssetResponse> result = service.findAll(bruno.getId());

        assertThat(result).hasSize(1);
        verify(assetRepository).findByPersonId(bruno.getId());
    }

    @Test
    void create_semPersonId_criaComSucesso() {
        var request = new AssetRequest("VALE3", AssetType.ACAO, new BigDecimal("314.48"), null);
        when(assetRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        AssetResponse response = service.create(request);

        assertThat(response.name()).isEqualTo("VALE3");
        assertThat(response.currentValue()).isEqualByComparingTo("314.48");
        assertThat(response.person()).isNull();
        verifyNoMoreInteractions(personRepository);
    }

    @Test
    void create_comPersonIdValido_associaPessoa() {
        var request = new AssetRequest("VALE3", AssetType.ACAO, new BigDecimal("314.48"), bruno.getId());
        when(personRepository.findById(bruno.getId())).thenReturn(Optional.of(bruno));
        when(assetRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        AssetResponse response = service.create(request);

        assertThat(response.person()).isNotNull();
        assertThat(response.person().id()).isEqualTo(bruno.getId());
    }

    @Test
    void create_comPersonIdInexistente_lancaBusinessException404() {
        UUID unknownPersonId = UUID.randomUUID();
        var request = new AssetRequest("VALE3", AssetType.ACAO, new BigDecimal("314.48"), unknownPersonId);
        when(personRepository.findById(unknownPersonId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.create(request))
                .isInstanceOf(BusinessException.class)
                .extracting("status").isEqualTo(HttpStatus.NOT_FOUND);

        verifyNoMoreInteractions(assetRepository);
    }

    @Test
    void update_ativoExistente_atualizaComSucesso() {
        var request = new AssetRequest("VALE3 Atualizado", AssetType.ACAO, new BigDecimal("400.00"), null);
        when(assetRepository.findById(vale3.getId())).thenReturn(Optional.of(vale3));
        when(assetRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        AssetResponse response = service.update(vale3.getId(), request);

        assertThat(response.name()).isEqualTo("VALE3 Atualizado");
        assertThat(response.currentValue()).isEqualByComparingTo("400.00");
    }

    @Test
    void update_ativoNaoEncontrado_lancaBusinessException404() {
        UUID id = UUID.randomUUID();
        when(assetRepository.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.update(id, new AssetRequest("X", AssetType.OUTRO, BigDecimal.ZERO, null)))
                .isInstanceOf(BusinessException.class)
                .extracting("status").isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void delete_ativoExistente_removeComSucesso() {
        when(assetRepository.findById(vale3.getId())).thenReturn(Optional.of(vale3));

        service.delete(vale3.getId());

        verify(assetRepository).delete(vale3);
    }

    @Test
    void delete_ativoNaoEncontrado_lancaBusinessException404() {
        UUID id = UUID.randomUUID();
        when(assetRepository.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.delete(id))
                .isInstanceOf(BusinessException.class)
                .extracting("status").isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void getNetWorth_comAtivos_retornaSoma() {
        when(assetRepository.sumCurrentValue()).thenReturn(new BigDecimal("314.48"));

        NetWorthResponse response = service.getNetWorth();

        assertThat(response.totalValue()).isEqualByComparingTo("314.48");
    }

    @Test
    void getNetWorth_semAtivos_retornaZero() {
        when(assetRepository.sumCurrentValue()).thenReturn(BigDecimal.ZERO);

        NetWorthResponse response = service.getNetWorth();

        assertThat(response.totalValue()).isEqualByComparingTo("0");
    }

    @Test
    void importFromXlsx_abaAcoes_criaAtivoComTipoAcao() throws IOException {
        MultipartFile file = workbook(Map.of(
                "Acoes", new Object[][]{
                        {"Produto", "Valor Atualizado"},
                        {"VALE3 - VALE S.A.", 314.48}
                }
        ));
        when(assetRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        AssetImportResponse response = service.importFromXlsx(file);

        assertThat(response.created()).hasSize(1);
        assertThat(response.updated()).isEmpty();
        assertThat(response.created().getFirst().name()).isEqualTo("VALE3 - VALE S.A.");
        assertThat(response.created().getFirst().type()).isEqualTo(AssetType.ACAO);
        assertThat(response.created().getFirst().currentValue()).isEqualByComparingTo("314.48");
        assertThat(response.errors()).isEmpty();
    }

    @Test
    void importFromXlsx_abaFundoDeInvestimento_criaAtivoComTipoFundo() throws IOException {
        MultipartFile file = workbook(Map.of(
                "Fundo de Investimento", new Object[][]{
                        {"Produto", "Valor Atualizado"},
                        {"BTCI11 - FII BTG PACTUAL CRÉDITO IMOBILIÁRIO RESP LIM", 204.60}
                }
        ));
        when(assetRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        AssetImportResponse response = service.importFromXlsx(file);

        assertThat(response.created()).hasSize(1);
        assertThat(response.created().getFirst().type()).isEqualTo(AssetType.FUNDO);
        assertThat(response.created().getFirst().currentValue()).isEqualByComparingTo("204.60");
        assertThat(response.errors()).isEmpty();
    }

    @Test
    void importFromXlsx_nomeETipoJaExistem_atualizaEmVezDeCriar() throws IOException {
        Asset existing = new Asset();
        existing.setId(UUID.randomUUID());
        existing.setName("VALE3 - VALE S.A.");
        existing.setType(AssetType.ACAO);
        existing.setCurrentValue(new BigDecimal("300.00"));

        MultipartFile file = workbook(Map.of(
                "Acoes", new Object[][]{
                        {"Produto", "Valor Atualizado"},
                        {"VALE3 - VALE S.A.", 314.48}
                }
        ));
        when(assetRepository.findByNameIgnoreCaseAndType("VALE3 - VALE S.A.", AssetType.ACAO))
                .thenReturn(List.of(existing));
        when(assetRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        AssetImportResponse response = service.importFromXlsx(file);

        assertThat(response.created()).isEmpty();
        assertThat(response.updated()).hasSize(1);
        assertThat(response.updated().getFirst().currentValue()).isEqualByComparingTo("314.48");
        assertThat(existing.getCurrentValue()).isEqualByComparingTo("314.48");
    }

    @Test
    void importFromXlsx_duasCorrespondenciasExistentes_atualizaAPrimeiraSemLancar() throws IOException {
        Asset first = new Asset();
        first.setId(UUID.randomUUID());
        first.setName("VALE3 - VALE S.A.");
        first.setType(AssetType.ACAO);
        first.setCurrentValue(new BigDecimal("100.00"));

        Asset second = new Asset();
        second.setId(UUID.randomUUID());
        second.setName("VALE3 - VALE S.A.");
        second.setType(AssetType.ACAO);
        second.setCurrentValue(new BigDecimal("200.00"));

        MultipartFile file = workbook(Map.of(
                "Acoes", new Object[][]{
                        {"Produto", "Valor Atualizado"},
                        {"VALE3 - VALE S.A.", 314.48}
                }
        ));
        when(assetRepository.findByNameIgnoreCaseAndType("VALE3 - VALE S.A.", AssetType.ACAO))
                .thenReturn(List.of(first, second));
        when(assetRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        AssetImportResponse response = service.importFromXlsx(file);

        assertThat(response.updated()).hasSize(1);
        assertThat(response.updated().getFirst().id()).isEqualTo(first.getId());
        assertThat(second.getCurrentValue()).isEqualByComparingTo("200.00");
    }

    @Test
    void importFromXlsx_linhaComProdutoVazio_viraErroSemCriarAtivo() throws IOException {
        // Regressão: o export de "Posição" da B3 sempre tem linhas de rodapé/subtotal no
        // final de cada aba com a coluna Produto vazia mas com valor numérico (o total da
        // aba) — sem essa validação, viravam "ativos fantasma" sem nome.
        MultipartFile file = workbook(Map.of(
                "Acoes", new Object[][]{
                        {"Produto", "Valor Atualizado"},
                        {"VALE3 - VALE S.A.", 314.48},
                        {"", 314.48}
                }
        ));
        when(assetRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        AssetImportResponse response = service.importFromXlsx(file);

        assertThat(response.created()).hasSize(1);
        assertThat(response.created().getFirst().name()).isEqualTo("VALE3 - VALE S.A.");
        assertThat(response.errors()).hasSize(1);
        assertThat(response.errors().getFirst().row()).isEqualTo(3);
        assertThat(response.errors().getFirst().isInformational()).isTrue();
    }

    @Test
    void importFromXlsx_linhaComProdutoSoEspacos_viraErroSemCriarAtivo() throws IOException {
        MultipartFile file = workbook(Map.of(
                "Acoes", new Object[][]{
                        {"Produto", "Valor Atualizado"},
                        {"   ", 100.0}
                }
        ));

        AssetImportResponse response = service.importFromXlsx(file);

        assertThat(response.created()).isEmpty();
        assertThat(response.errors()).hasSize(1);
        assertThat(response.errors().getFirst().isInformational()).isTrue();
        verifyNoMoreInteractions(assetRepository);
    }

    @Test
    void importFromXlsx_nomeComEspacosNasBordas_salvaSemEspacos() throws IOException {
        MultipartFile file = workbook(Map.of(
                "Acoes", new Object[][]{
                        {"Produto", "Valor Atualizado"},
                        {"  VALE3 - VALE S.A.  ", 314.48}
                }
        ));
        when(assetRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        AssetImportResponse response = service.importFromXlsx(file);

        assertThat(response.created().getFirst().name()).isEqualTo("VALE3 - VALE S.A.");
    }

    @Test
    void importFromXlsx_linhaComValorNegativo_viraErroSemCriarAtivo() throws IOException {
        MultipartFile file = workbook(Map.of(
                "Acoes", new Object[][]{
                        {"Produto", "Valor Atualizado"},
                        {"VALE3 - VALE S.A.", -1.0}
                }
        ));

        AssetImportResponse response = service.importFromXlsx(file);

        assertThat(response.created()).isEmpty();
        assertThat(response.errors()).hasSize(1);
        assertThat(response.errors().getFirst().isInformational()).isFalse();
        verifyNoMoreInteractions(assetRepository);
    }

    @Test
    void importFromXlsx_abaRendaFixa_semMtmNemFechamento_usaValorCurva() throws IOException {
        MultipartFile file = workbook(Map.of(
                "Renda Fixa", new Object[][]{
                        {"Produto", "Valor Atualizado MTM", "Valor Atualizado CURVA", "Valor Atualizado FECHAMENTO"},
                        {"CDB - ITAU UNIBANCO S.A.", "-", 212.17, "-"}
                }
        ));
        when(assetRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        AssetImportResponse response = service.importFromXlsx(file);

        assertThat(response.created()).hasSize(1);
        assertThat(response.created().getFirst().type()).isEqualTo(AssetType.RENDA_FIXA);
        assertThat(response.created().getFirst().currentValue()).isEqualByComparingTo("212.17");
        assertThat(response.errors()).isEmpty();
    }

    @Test
    void importFromXlsx_linhaSemValorUtilizavel_viraErroSemCriarAtivo() throws IOException {
        MultipartFile file = workbook(Map.of(
                "Renda Fixa", new Object[][]{
                        {"Produto", "Valor Atualizado MTM", "Valor Atualizado CURVA", "Valor Atualizado FECHAMENTO"},
                        {"CDB - ITAU UNIBANCO S.A.", "-", "-", "-"}
                }
        ));

        AssetImportResponse response = service.importFromXlsx(file);

        assertThat(response.created()).isEmpty();
        assertThat(response.errors()).hasSize(1);
        assertThat(response.errors().getFirst().sheet()).isEqualTo("Renda Fixa");
        assertThat(response.errors().getFirst().isInformational()).isFalse();
        verifyNoMoreInteractions(assetRepository);
    }

    @Test
    void importFromXlsx_linhaSemValorMisturadaComLinhaValida_importaAValidaEReportaAInvalida() throws IOException {
        MultipartFile file = workbook(Map.of(
                "Renda Fixa", new Object[][]{
                        {"Produto", "Valor Atualizado MTM", "Valor Atualizado CURVA", "Valor Atualizado FECHAMENTO"},
                        {"CDB - ITAU UNIBANCO S.A.", "-", 212.17, "-"},
                        {"CDB - SEM VALOR", "-", "-", "-"}
                }
        ));
        when(assetRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        AssetImportResponse response = service.importFromXlsx(file);

        assertThat(response.created()).hasSize(1);
        assertThat(response.created().getFirst().name()).isEqualTo("CDB - ITAU UNIBANCO S.A.");
        assertThat(response.errors()).hasSize(1);
        assertThat(response.errors().getFirst().message()).isEqualTo("Valor atualizado indisponível");
    }

    @Test
    void importFromXlsx_abaTesouroDireto_usaColunaValorAtualizadoSimples() throws IOException {
        // Regressão: Tesouro Direto também vira AssetType.RENDA_FIXA, mas sua planilha usa
        // a coluna "Valor Atualizado" simples (como Acoes/Fundo), não as variantes
        // MTM/CURVA/FECHAMENTO exclusivas da aba "Renda Fixa".
        MultipartFile file = workbook(Map.of(
                "Tesouro Direto", new Object[][]{
                        {"Produto", "Valor Atualizado"},
                        {"Tesouro Prefixado 2029", 37.02}
                }
        ));
        when(assetRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        AssetImportResponse response = service.importFromXlsx(file);

        assertThat(response.errors()).isEmpty();
        assertThat(response.created()).hasSize(1);
        assertThat(response.created().getFirst().type()).isEqualTo(AssetType.RENDA_FIXA);
        assertThat(response.created().getFirst().currentValue()).isEqualByComparingTo("37.02");
    }

    @Test
    void importFromXlsx_abaDesconhecida_viraErroSemAbortarImport() throws IOException {
        MultipartFile file = workbook(Map.of(
                "Acoes", new Object[][]{
                        {"Produto", "Valor Atualizado"},
                        {"VALE3 - VALE S.A.", 314.48}
                },
                "Outra Aba", new Object[][]{
                        {"Produto", "Valor Atualizado"},
                        {"Item Qualquer", 10.0}
                }
        ));
        when(assetRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        AssetImportResponse response = service.importFromXlsx(file);

        assertThat(response.created()).hasSize(1);
        assertThat(response.errors()).hasSize(1);
        assertThat(response.errors().getFirst().sheet()).isEqualTo("Outra Aba");
        assertThat(response.errors().getFirst().isInformational()).isFalse();
    }

    @Test
    void importFromXlsx_arquivoVazio_lancaBusinessException400() {
        MultipartFile file = new MockMultipartFile("file", "posicao.xlsx", null, new byte[0]);

        assertThatThrownBy(() -> service.importFromXlsx(file))
                .isInstanceOf(BusinessException.class)
                .extracting("status").isEqualTo(HttpStatus.BAD_REQUEST);

        verifyNoMoreInteractions(assetRepository);
    }

    @Test
    void importFromXlsx_arquivoNaoEhXlsx_lancaBusinessException400() {
        // Ex.: usuário envia sem querer a versão CSV/PDF de "Posição" da B3
        MultipartFile file = new MockMultipartFile("file", "posicao.csv", null, "não é um xlsx".getBytes());

        assertThatThrownBy(() -> service.importFromXlsx(file))
                .isInstanceOf(BusinessException.class)
                .extracting("status").isEqualTo(HttpStatus.BAD_REQUEST);

        verifyNoMoreInteractions(assetRepository);
    }

    @Test
    void importFromXlsx_produtoComCelulaNumerica_naoLancaExcecao() throws IOException {
        // Regressão: cellAsString usava getStringCellValue(), que lança IllegalStateException
        // em qualquer célula que não seja do tipo STRING — uma célula numérica na coluna
        // Produto derrubava o import inteiro em vez de virar um erro reportado.
        MultipartFile file = workbook(Map.of(
                "Acoes", new Object[][]{
                        {"Produto", "Valor Atualizado"},
                        {123.0, 314.48}
                }
        ));
        when(assetRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        AssetImportResponse response = service.importFromXlsx(file);

        assertThat(response.created()).hasSize(1);
    }

    private MultipartFile workbook(Map<String, Object[][]> sheets) throws IOException {
        try (XSSFWorkbook wb = new XSSFWorkbook()) {
            for (Map.Entry<String, Object[][]> entry : sheets.entrySet()) {
                Sheet sheet = wb.createSheet(entry.getKey());
                int rowIndex = 0;
                for (Object[] rowValues : entry.getValue()) {
                    Row row = sheet.createRow(rowIndex++);
                    for (int col = 0; col < rowValues.length; col++) {
                        Object value = rowValues[col];
                        if (value instanceof Number number) {
                            row.createCell(col).setCellValue(number.doubleValue());
                        } else {
                            row.createCell(col).setCellValue(value.toString());
                        }
                    }
                }
            }

            ByteArrayOutputStream out = new ByteArrayOutputStream();
            wb.write(out);
            return new MockMultipartFile("file", "posicao.xlsx", null, out.toByteArray());
        }
    }
}
