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
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
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
        when(assetRepository.findByNameIgnoreCaseAndTypeAndExternalCodeIsNull("VALE3 - VALE S.A.", AssetType.ACAO))
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
        when(assetRepository.findByNameIgnoreCaseAndTypeAndExternalCodeIsNull("VALE3 - VALE S.A.", AssetType.ACAO))
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

    @Test
    void importFromXlsx_falhaInesperadaNumaLinha_importaAsOutrasEReportaAQuebrada() throws IOException {
        // Falha inesperada (não é linha inválida prevista): o banco recusa a gravação. Antes
        // da #40 isso derrubava o import inteiro e ainda dizia que o arquivo era inválido,
        // deixando as linhas anteriores já commitadas
        MultipartFile file = workbook(Map.of(
                "Acoes", new Object[][]{
                        {"Produto", "Código de Negociação", "Valor Atualizado"},
                        {"XPTO3 - XPTO S.A.", "XPTO3", 290.20},
                        {"XPTO4 - XPTO PART S.A.", "XPTO4", 109.80}
                }
        ));
        when(assetRepository.save(any()))
                .thenAnswer(inv -> inv.getArgument(0))
                .thenThrow(new DataIntegrityViolationException("valor muito longo para a coluna name"));

        AssetImportResponse response = service.importFromXlsx(file);

        assertThat(response.created()).hasSize(1);
        assertThat(response.errors()).singleElement()
                .satisfies(error -> {
                    assertThat(error.sheet()).isEqualTo("Acoes");
                    assertThat(error.isInformational()).isFalse();
                });
    }

    @Test
    void importFromXlsx_falhaInesperadaNumaLinha_naoEntraNaReconciliacao() throws IOException {
        // a linha que não foi gravada também não pode contar como lida, senão a reconciliação
        // acusaria uma divergência que não existe e mandaria o usuário caçar fantasma
        MultipartFile file = workbook(Map.of(
                "Acoes", new Object[][]{
                        {"Produto", "Código de Negociação", "Valor Atualizado"},
                        {"XPTO3 - XPTO S.A.", "XPTO3", 290.20},
                        {"XPTO4 - XPTO PART S.A.", "XPTO4", 109.80}
                }
        ));
        when(assetRepository.save(any()))
                .thenAnswer(inv -> inv.getArgument(0))
                .thenThrow(new DataIntegrityViolationException("falha simulada"));

        AssetImportResponse response = service.importFromXlsx(file);

        assertThat(response.totalRead()).isEqualByComparingTo("290.20");
        assertThat(response.totalPersisted()).isEqualByComparingTo("290.20");
        assertThat(response.sheets()).singleElement()
                .satisfies(sheet -> assertThat(sheet.rows()).isEqualTo(1));
    }

    @Test
    void importFromXlsx_duasLinhasMesmoProdutoCodigosDiferentes_criaDoisAtivos() throws IOException {
        // O bug da #37: duas aplicações no mesmo papel do mesmo emissor, distintas só pela
        // coluna Código, viravam um ativo só com o valor da última linha lida — o resto
        // desaparecia do patrimônio sem nenhum sinal na resposta do import
        MultipartFile file = workbook(Map.of(
                "Renda Fixa", new Object[][]{
                        {"Produto", "Código", "Valor Atualizado MTM", "Valor Atualizado CURVA", "Valor Atualizado FECHAMENTO"},
                        {"CDB - BANCO XPTO S.A.", "CDB111AA11A", "-", 1000.00, "-"},
                        {"CDB - BANCO XPTO S.A.", "CDB222BB22B", "-", 2500.00, "-"}
                }
        ));
        // repositório com estado: sem ele o mock devolveria lista vazia em toda busca,
        // nenhum upsert aconteceria, e as duas linhas virariam ativos distintos mesmo com o
        // bug presente — o teste passaria sem provar nada
        stubStatefulRepository();

        AssetImportResponse response = service.importFromXlsx(file);

        assertThat(response.created()).hasSize(2);
        assertThat(sumOf(response.created())).isEqualByComparingTo("3500.00");
        assertThat(response.sheets()).singleElement()
                .satisfies(sheet -> assertThat(sheet.totalPersisted()).isEqualByComparingTo(sheet.totalRead()));
    }

    @Test
    void importFromXlsx_ativoLegadoSemCodigo_adotaOCodigoEmVezDeDuplicar() throws IOException {
        // Ativos importados antes da #37 não têm external_code; o primeiro import depois do
        // deploy adota o código pelo nome, sem backfill na migration
        Asset legado = new Asset();
        legado.setId(UUID.randomUUID());
        legado.setName("XPTO11 - FUNDO XPTO");
        legado.setType(AssetType.FUNDO);
        legado.setCurrentValue(new BigDecimal("100.00"));

        MultipartFile file = workbook(Map.of(
                "Fundo de Investimento", new Object[][]{
                        {"Produto", "Código de Negociação", "Valor Atualizado"},
                        {"XPTO11 - FUNDO XPTO", "XPTO11", 250.00}
                }
        ));
        when(assetRepository.findByExternalCode("XPTO11")).thenReturn(List.of());
        when(assetRepository.findByNameIgnoreCaseAndTypeAndExternalCodeIsNull("XPTO11 - FUNDO XPTO", AssetType.FUNDO))
                .thenReturn(List.of(legado));
        when(assetRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        AssetImportResponse response = service.importFromXlsx(file);

        assertThat(response.created()).isEmpty();
        assertThat(response.updated()).hasSize(1);
        assertThat(legado.getExternalCode()).isEqualTo("XPTO11");
        assertThat(legado.getCurrentValue()).isEqualByComparingTo("250.00");
    }

    @Test
    void importFromXlsx_ativoLegadoJaAdotado_naoEhRoubadoPorOutraLinha() throws IOException {
        // É o que impede o bug de voltar pela porta dos fundos: assim que a primeira linha
        // adota o ativo legado, ele deixa de ser candidato do fallback por nome, e a segunda
        // linha vira ativo novo em vez de sobrescrever a primeira
        Asset legado = new Asset();
        legado.setId(UUID.randomUUID());
        legado.setName("CDB - BANCO XPTO S.A.");
        legado.setType(AssetType.RENDA_FIXA);
        legado.setCurrentValue(new BigDecimal("100.00"));

        MultipartFile file = workbook(Map.of(
                "Renda Fixa", new Object[][]{
                        {"Produto", "Código", "Valor Atualizado MTM", "Valor Atualizado CURVA", "Valor Atualizado FECHAMENTO"},
                        {"CDB - BANCO XPTO S.A.", "CDB111AA11A", "-", 1000.00, "-"},
                        {"CDB - BANCO XPTO S.A.", "CDB222BB22B", "-", 2500.00, "-"}
                }
        ));
        when(assetRepository.findByExternalCode(any())).thenReturn(List.of());
        when(assetRepository.findByNameIgnoreCaseAndTypeAndExternalCodeIsNull("CDB - BANCO XPTO S.A.", AssetType.RENDA_FIXA))
                .thenReturn(List.of(legado))
                .thenReturn(List.of());
        when(assetRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        AssetImportResponse response = service.importFromXlsx(file);

        assertThat(response.updated()).hasSize(1);
        assertThat(response.created()).hasSize(1);
        assertThat(legado.getExternalCode()).isEqualTo("CDB111AA11A");
        assertThat(legado.getCurrentValue()).isEqualByComparingTo("1000.00");
    }

    @Test
    void importFromXlsx_produtoRenomeadoMesmoCodigo_atualizaEmVezDeDuplicar() throws IOException {
        Asset existente = new Asset();
        existente.setId(UUID.randomUUID());
        existente.setName("XPTO11 - NOME ANTIGO");
        existente.setType(AssetType.FUNDO);
        existente.setExternalCode("XPTO11");
        existente.setCurrentValue(new BigDecimal("100.00"));

        MultipartFile file = workbook(Map.of(
                "Fundo de Investimento", new Object[][]{
                        {"Produto", "Código de Negociação", "Valor Atualizado"},
                        {"XPTO11 - NOME NOVO", "XPTO11", 250.00}
                }
        ));
        when(assetRepository.findByExternalCode("XPTO11")).thenReturn(List.of(existente));
        when(assetRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        AssetImportResponse response = service.importFromXlsx(file);

        assertThat(response.created()).isEmpty();
        assertThat(response.updated()).hasSize(1);
        assertThat(existente.getName()).isEqualTo("XPTO11 - NOME NOVO");
    }

    @Test
    void importFromXlsx_reconciliacao_totalLidoIgualAoPersistidoPorAba() throws IOException {
        MultipartFile file = workbook(Map.of(
                "Acoes", new Object[][]{
                        {"Produto", "Código de Negociação", "Valor Atualizado"},
                        {"XPTO3 - XPTO S.A.", "XPTO3", 290.20},
                        {"XPTO4 - XPTO PART S.A.", "XPTO4", 109.80}
                }
        ));
        stubStatefulRepository();

        AssetImportResponse response = service.importFromXlsx(file);

        assertThat(response.sheets()).singleElement()
                .satisfies(sheet -> {
                    assertThat(sheet.sheet()).isEqualTo("Acoes");
                    assertThat(sheet.rows()).isEqualTo(2);
                    assertThat(sheet.totalRead()).isEqualByComparingTo("400.00");
                    assertThat(sheet.totalPersisted()).isEqualByComparingTo("400.00");
                });
        assertThat(response.totalRead()).isEqualByComparingTo("400.00");
        assertThat(response.totalPersisted()).isEqualByComparingTo("400.00");
    }

    @Test
    void importFromXlsx_duasLinhasComMesmoCodigo_reportaDivergenciaNaReconciliacao() throws IOException {
        // A limitação documentada: dois papéis compartilhando código colapsam num ativo só.
        // A reconciliação existe pra isso não passar em silêncio — não basta os totais
        // divergirem num campo JSON, tem que sair em `errors`, que é o que a tela mostra
        MultipartFile file = workbook(Map.of(
                "Renda Fixa", new Object[][]{
                        {"Produto", "Código", "Valor Atualizado MTM", "Valor Atualizado CURVA", "Valor Atualizado FECHAMENTO"},
                        {"CDB - BANCO XPTO S.A.", "CDB111AA11A", "-", 1000.00, "-"},
                        {"CDB - BANCO YPTO S.A.", "CDB111AA11A", "-", 2500.00, "-"}
                }
        ));
        stubStatefulRepository();

        AssetImportResponse response = service.importFromXlsx(file);

        assertThat(response.totalRead()).isEqualByComparingTo("3500.00");
        assertThat(response.totalPersisted()).isEqualByComparingTo("2500.00");
        assertThat(response.errors()).singleElement()
                .satisfies(error -> {
                    assertThat(error.sheet()).isEqualTo("Renda Fixa");
                    assertThat(error.isInformational()).isFalse();
                    assertThat(error.message()).contains("diverge");
                });
    }

    @Test
    void importFromXlsx_linhaDeRodape_naoEntraNosTotais() throws IOException {
        // a linha "Total" da B3 tem Produto vazio e repete o total da aba — se entrasse na
        // reconciliação, todo import fecharia com o dobro do valor
        MultipartFile file = workbook(Map.of(
                "Acoes", new Object[][]{
                        {"Produto", "Código de Negociação", "Valor Atualizado"},
                        {"XPTO3 - XPTO S.A.", "XPTO3", 290.20},
                        {"", "", 290.20}
                }
        ));
        when(assetRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        AssetImportResponse response = service.importFromXlsx(file);

        assertThat(response.totalRead()).isEqualByComparingTo("290.20");
        assertThat(response.errors()).singleElement()
                .satisfies(error -> assertThat(error.isInformational()).isTrue());
    }

    @Test
    void importFromXlsx_ativoComCodigoAusenteDoArquivo_entraEmMissingSemSerApagado() throws IOException {
        Asset vendido = new Asset();
        vendido.setId(UUID.randomUUID());
        vendido.setName("XPTO4 - XPTO S.A.");
        vendido.setType(AssetType.ACAO);
        vendido.setExternalCode("XPTO4");
        vendido.setCurrentValue(new BigDecimal("500.00"));

        MultipartFile file = workbook(Map.of(
                "Acoes", new Object[][]{
                        {"Produto", "Código de Negociação", "Valor Atualizado"},
                        {"XPTO3 - XPTO S.A.", "XPTO3", 290.20}
                }
        ));
        when(assetRepository.findByExternalCodeIsNotNull()).thenReturn(List.of(vendido));
        when(assetRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        AssetImportResponse response = service.importFromXlsx(file);

        assertThat(response.missing()).singleElement()
                .extracting(AssetResponse::externalCode).isEqualTo("XPTO4");
        verify(assetRepository, never()).delete(any());
    }

    @Test
    void importFromXlsx_linhaComValorIlegivel_naoReportaOAtivoDelaComoMissing() throws IOException {
        // O ativo ESTÁ no arquivo e continua sendo do usuário — só a célula de valor veio
        // ilegível (título vencido/bloqueado manda "-" também na coluna CURVA). Reportá-lo
        // em `missing` convida a apagar um ativo que ele tem: `missing` significa "não
        // apareceu no extrato", não "não deu pra ler o valor"
        Asset existente = new Asset();
        existente.setId(UUID.randomUUID());
        existente.setName("CDB - BANCO XPTO S.A.");
        existente.setType(AssetType.RENDA_FIXA);
        existente.setExternalCode("CDB111AA11A");
        existente.setCurrentValue(new BigDecimal("1000.00"));

        MultipartFile file = workbook(Map.of(
                "Renda Fixa", new Object[][]{
                        {"Produto", "Código", "Valor Atualizado MTM", "Valor Atualizado CURVA", "Valor Atualizado FECHAMENTO"},
                        {"CDB - BANCO XPTO S.A.", "CDB111AA11A", "-", "-", "-"},
                        {"CDB - BANCO YPTO S.A.", "CDB222BB22B", "-", 2500.00, "-"}
                }
        ));
        when(assetRepository.findByExternalCodeIsNotNull()).thenReturn(List.of(existente));
        when(assetRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        AssetImportResponse response = service.importFromXlsx(file);

        assertThat(response.errors()).singleElement()
                .satisfies(error -> assertThat(error.message()).isEqualTo("Valor atualizado indisponível"));
        assertThat(response.missing()).isEmpty();
    }

    @Test
    void importFromXlsx_abaSemColunaDeCodigo_naoReportaNadaComoMissing() throws IOException {
        // Se a B3 renomear o cabeçalho (ou vier um export de formato antigo), nenhuma linha
        // da aba registra código — mas as linhas ainda importam pelo fallback por nome, então
        // o early-return de created/updated não salva. Sem o guard, todo ativo com código de
        // imports anteriores apareceria como "sumiu do extrato"
        Asset existente = new Asset();
        existente.setId(UUID.randomUUID());
        existente.setName("XPTO4 - XPTO PART S.A.");
        existente.setType(AssetType.ACAO);
        existente.setExternalCode("XPTO4");
        existente.setCurrentValue(new BigDecimal("500.00"));

        MultipartFile file = workbook(Map.of(
                "Acoes", new Object[][]{
                        {"Produto", "Valor Atualizado"},
                        {"XPTO3 - XPTO S.A.", 290.20}
                }
        ));
        when(assetRepository.findByExternalCodeIsNotNull()).thenReturn(List.of(existente));
        when(assetRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        AssetImportResponse response = service.importFromXlsx(file);

        assertThat(response.created()).hasSize(1);
        assertThat(response.missing()).isEmpty();
        assertThat(response.errors()).singleElement()
                .satisfies(error -> {
                    assertThat(error.isInformational()).isFalse();
                    assertThat(error.message()).contains("Coluna de código não encontrada");
                });
    }

    @Test
    void importFromXlsx_ativoManualSemCodigo_naoEntraEmMissing() throws IOException {
        // imóvel/cripto cadastrado à mão nunca veio de extrato, então "não está no arquivo"
        // não diz nada sobre ele
        MultipartFile file = workbook(Map.of(
                "Acoes", new Object[][]{
                        {"Produto", "Código de Negociação", "Valor Atualizado"},
                        {"XPTO3 - XPTO S.A.", "XPTO3", 290.20}
                }
        ));
        when(assetRepository.findByExternalCodeIsNotNull()).thenReturn(List.of());
        when(assetRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        AssetImportResponse response = service.importFromXlsx(file);

        assertThat(response.missing()).isEmpty();
    }

    @Test
    void importFromXlsx_mesmoArquivoDuasVezes_atualizaSemDuplicarEMantemOTotal() throws IOException {
        // idempotência do caminho novo, com código: o teste antigo
        // (importarPosicaoB3_duasVezes_atualizaEmVezDeDuplicar) usa uma planilha sem coluna
        // de código e só exercita o fallback por nome
        MultipartFile file = workbook(Map.of(
                "Renda Fixa", new Object[][]{
                        {"Produto", "Código", "Valor Atualizado MTM", "Valor Atualizado CURVA", "Valor Atualizado FECHAMENTO"},
                        {"CDB - BANCO XPTO S.A.", "CDB111AA11A", "-", 1000.00, "-"},
                        {"CDB - BANCO XPTO S.A.", "CDB222BB22B", "-", 2500.00, "-"}
                }
        ));
        stubStatefulRepository();

        service.importFromXlsx(file);
        AssetImportResponse segunda = service.importFromXlsx(file);

        assertThat(segunda.created()).isEmpty();
        assertThat(segunda.updated()).hasSize(2);
        assertThat(segunda.totalPersisted()).isEqualByComparingTo("3500.00");
        assertThat(segunda.totalPersisted()).isEqualByComparingTo(segunda.totalRead());
    }

    /**
     * Faz o mock do repositório se comportar como um banco de verdade durante o import: o
     * que foi salvo é encontrado pelas buscas seguintes. Sem isso, toda busca devolve vazio
     * e nenhum caminho de upsert (o que o bug da #37 vive) chega a ser exercitado.
     *
     * <p>Use este fake em qualquer teste que crie mais de um ativo na mesma aba. O stub de
     * eco (<code>thenAnswer(inv -&gt; inv.getArgument(0))</code>) devolve entidade com id
     * nulo, e duas chaves nulas viram uma só na reconciliação — o teste acusaria um colapso
     * que não existe.
     */
    private void stubStatefulRepository() {
        List<Asset> db = new ArrayList<>();

        when(assetRepository.save(any())).thenAnswer(invocation -> {
            Asset asset = invocation.getArgument(0);
            // o save real nunca devolve entidade sem id; sem isso o fake esconderia um
            // colapso de linhas (duas chaves nulas viram uma só na reconciliação)
            if (asset.getId() == null) {
                asset.setId(UUID.randomUUID());
            }
            if (db.stream().noneMatch(saved -> saved == asset)) {
                db.add(asset);
            }
            return asset;
        });
        when(assetRepository.findByExternalCode(any())).thenAnswer(invocation ->
                db.stream()
                        .filter(asset -> invocation.getArgument(0).equals(asset.getExternalCode()))
                        .toList());
        when(assetRepository.findByNameIgnoreCaseAndTypeAndExternalCodeIsNull(any(), any())).thenAnswer(invocation ->
                db.stream()
                        .filter(asset -> asset.getExternalCode() == null)
                        .filter(asset -> asset.getName().equalsIgnoreCase(invocation.getArgument(0)))
                        .filter(asset -> asset.getType() == invocation.getArgument(1))
                        .toList());
    }

    private BigDecimal sumOf(List<AssetResponse> assets) {
        return assets.stream().map(AssetResponse::currentValue).reduce(BigDecimal.ZERO, BigDecimal::add);
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
