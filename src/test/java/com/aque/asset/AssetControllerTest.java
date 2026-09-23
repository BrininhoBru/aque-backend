package com.aque.asset;

import com.aque.BaseIntegrationTest;
import com.aque.asset.dto.request.AssetRequest;
import com.aque.person.Person;
import com.aque.person.PersonRepository;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;

import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@AutoConfigureMockMvc
class AssetControllerTest extends BaseIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private AssetRepository assetRepository;

    @Autowired
    private PersonRepository personRepository;

    private Asset vale3;
    private Person bruno;

    @BeforeEach
    void setupAssets() {
        assetRepository.deleteAll();
        personRepository.deleteAll();

        bruno = new Person();
        bruno.setName("Bruno");
        personRepository.save(bruno);

        vale3 = new Asset();
        vale3.setName("VALE3");
        vale3.setType(AssetType.ACAO);
        vale3.setCurrentValue(new BigDecimal("314.48"));
        vale3.setPerson(bruno);
        assetRepository.save(vale3);

        // sem pessoa — prova que o filtro por personId realmente exclui, não só "não quebra"
        Asset semPessoa = new Asset();
        semPessoa.setName("BTCI11");
        semPessoa.setType(AssetType.FUNDO);
        semPessoa.setCurrentValue(new BigDecimal("204.60"));
        assetRepository.save(semPessoa);
    }

    @Test
    void listarAtivos_deveRetornarTodos() throws Exception {
        mockMvc.perform(get("/assets")
                        .header("Authorization", token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2));
    }

    @Test
    void listarAtivos_filtrandoPorPersonId() throws Exception {
        mockMvc.perform(get("/assets?personId=" + bruno.getId())
                        .header("Authorization", token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].name").value("VALE3"));
    }

    @Test
    void criarAtivo_deveRetornar201() throws Exception {
        String body = objectMapper.writeValueAsString(
                new AssetRequest("BTCI11", AssetType.FUNDO, new BigDecimal("204.60"), null));

        mockMvc.perform(post("/assets")
                        .header("Authorization", token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("BTCI11"))
                .andExpect(jsonPath("$.person").doesNotExist());
    }

    @Test
    void criarAtivo_comValorNegativo_deveRetornar400() throws Exception {
        String body = objectMapper.writeValueAsString(
                new AssetRequest("BTCI11", AssetType.FUNDO, new BigDecimal("-1.00"), null));

        mockMvc.perform(post("/assets")
                        .header("Authorization", token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest());
    }

    @Test
    void criarAtivo_semTipo_deveRetornar400() throws Exception {
        String body = objectMapper.writeValueAsString(
                new AssetRequest("BTCI11", null, new BigDecimal("204.60"), null));

        mockMvc.perform(post("/assets")
                        .header("Authorization", token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest());
    }

    @Test
    void criarAtivo_comTipoForaDoEnum_deveRetornar400() throws Exception {
        // Diferente de "sem tipo" (violação de @NotNull) — aqui o valor existe mas não bate
        // com nenhuma constante de AssetType, falhando na desserialização do Jackson
        // (HttpMessageNotReadableException), não na validação Bean Validation.
        String body = "{\"name\":\"BTCI11\",\"type\":\"BITCOIN\",\"currentValue\":204.60,\"personId\":null}";

        mockMvc.perform(post("/assets")
                        .header("Authorization", token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest());
    }

    @Test
    void editarAtivo_deveRetornar200() throws Exception {
        String body = objectMapper.writeValueAsString(
                new AssetRequest("VALE3 Atualizado", AssetType.ACAO, new BigDecimal("400.00"), null));

        mockMvc.perform(put("/assets/" + vale3.getId())
                        .header("Authorization", token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("VALE3 Atualizado"));
    }

    @Test
    void editarAtivo_mudandoTipo_deveRefletirNovoTipo() throws Exception {
        String body = objectMapper.writeValueAsString(
                new AssetRequest("VALE3", AssetType.OUTRO, new BigDecimal("314.48"), null));

        mockMvc.perform(put("/assets/" + vale3.getId())
                        .header("Authorization", token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.type").value("OUTRO"));
    }

    @Test
    void editarAtivoInexistente_deveRetornar404() throws Exception {
        String body = objectMapper.writeValueAsString(
                new AssetRequest("X", AssetType.OUTRO, BigDecimal.ZERO, null));

        mockMvc.perform(put("/assets/00000000-0000-0000-0000-000000000000")
                        .header("Authorization", token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isNotFound());
    }

    @Test
    void ativoNaoEncontrado_deveRetornar404() throws Exception {
        mockMvc.perform(delete("/assets/00000000-0000-0000-0000-000000000000")
                        .header("Authorization", token))
                .andExpect(status().isNotFound());
    }

    @Test
    void excluirAtivo_deveRetornar204() throws Exception {
        mockMvc.perform(delete("/assets/" + vale3.getId())
                        .header("Authorization", token))
                .andExpect(status().isNoContent());
    }

    @Test
    void patrimonioTotal_deveRetornarSoma() throws Exception {
        mockMvc.perform(get("/assets/net-worth")
                        .header("Authorization", token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalValue").value(519.08));
    }

    @Test
    void importarPosicaoB3_deveCriarAtivosERetornar200() throws Exception {
        MockMultipartFile file = posicaoAcoesFile("VALE3 - VALE S.A.", 314.48);

        mockMvc.perform(multipart("/assets/import")
                        .file(file)
                        .header("Authorization", token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.created.length()").value(1))
                .andExpect(jsonPath("$.created[0].name").value("VALE3 - VALE S.A."))
                .andExpect(jsonPath("$.updated.length()").value(0))
                .andExpect(jsonPath("$.errors.length()").value(0));
    }

    @Test
    void importarPosicaoB3_duasVezes_atualizaEmVezDeDuplicar() throws Exception {
        MockMultipartFile file = posicaoAcoesFile("VALE3 - VALE S.A.", 314.48);
        mockMvc.perform(multipart("/assets/import").file(file).header("Authorization", token))
                .andExpect(status().isOk());

        MockMultipartFile fileAtualizado = posicaoAcoesFile("VALE3 - VALE S.A.", 400.00);
        mockMvc.perform(multipart("/assets/import")
                        .file(fileAtualizado)
                        .header("Authorization", token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.created.length()").value(0))
                .andExpect(jsonPath("$.updated.length()").value(1))
                .andExpect(jsonPath("$.updated[0].currentValue").value(400.00));

        mockMvc.perform(get("/assets").header("Authorization", token))
                .andExpect(jsonPath("$.length()").value(3)); // vale3 + semPessoa do setup + VALE3 - VALE S.A. importado (nomes diferentes)
    }

    @Test
    void importarPosicaoB3_comNomeEmCaixaDiferente_atualizaAtivoExistente() throws Exception {
        // fixture tem "VALE3" (maiúsculas) — importa em minúsculas pra provar que o
        // casamento por nome+tipo é case-insensitive de verdade, não só por delegação
        MockMultipartFile file = posicaoAcoesFile("vale3", 500.00);

        mockMvc.perform(multipart("/assets/import")
                        .file(file)
                        .header("Authorization", token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.created.length()").value(0))
                .andExpect(jsonPath("$.updated.length()").value(1))
                .andExpect(jsonPath("$.updated[0].currentValue").value(500.00));

        mockMvc.perform(get("/assets").header("Authorization", token))
                .andExpect(jsonPath("$.length()").value(2)); // sem duplicar
    }

    @Test
    void importarPosicaoB3_duasAplicacoesNoMesmoPapel_patrimonioTotalBateComOArquivo() throws Exception {
        // O bug da #37 ponta a ponta, com Postgres de verdade: duas aplicações no mesmo
        // papel distintas só pelo Código colapsavam num ativo só, e o patrimônio total
        // ficava menor que o do extrato sem nada denunciar
        MockMultipartFile file = posicaoRendaFixaFile();

        mockMvc.perform(multipart("/assets/import")
                        .file(file)
                        .header("Authorization", token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.created.length()").value(2))
                .andExpect(jsonPath("$.totalRead").value(3500.00))
                .andExpect(jsonPath("$.totalPersisted").value(3500.00));

        // 3500 do arquivo + 314.48 + 204.60 do fixture
        mockMvc.perform(get("/assets/net-worth").header("Authorization", token))
                .andExpect(jsonPath("$.totalValue").value(4019.08));
    }

    @Test
    void importarPosicaoB3_mesmoArquivoComCodigoDuasVezes_naoDuplicaEMantemOPatrimonio() throws Exception {
        mockMvc.perform(multipart("/assets/import").file(posicaoRendaFixaFile()).header("Authorization", token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.created.length()").value(2));

        mockMvc.perform(multipart("/assets/import")
                        .file(posicaoRendaFixaFile())
                        .header("Authorization", token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.created.length()").value(0))
                .andExpect(jsonPath("$.updated.length()").value(2))
                .andExpect(jsonPath("$.missing.length()").value(0));

        mockMvc.perform(get("/assets").header("Authorization", token))
                .andExpect(jsonPath("$.length()").value(4)); // 2 do fixture + 2 importados
        mockMvc.perform(get("/assets/net-worth").header("Authorization", token))
                .andExpect(jsonPath("$.totalValue").value(4019.08));
    }

    @Test
    void importarPosicaoB3_ativoDeImportAnteriorAusente_entraEmMissingSemSerApagado() throws Exception {
        mockMvc.perform(multipart("/assets/import").file(posicaoRendaFixaFile()).header("Authorization", token))
                .andExpect(status().isOk());

        // segundo extrato sem a renda fixa: o ativo não pode sumir do banco sozinho
        mockMvc.perform(multipart("/assets/import")
                        .file(posicaoAcoesFile("XPTO3 - XPTO S.A.", 290.20))
                        .header("Authorization", token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.missing.length()").value(2));

        mockMvc.perform(get("/assets").header("Authorization", token))
                .andExpect(jsonPath("$.length()").value(5));
    }

    @Test
    void importarPosicaoB3_linhaRecusadaPeloBanco_importaAsOutrasEReportaAQuebrada() throws Exception {
        // gatilho de verdade, não mock: o VARCHAR(255) da V7 recusa este nome. Antes da #40
        // isso derrubava o import inteiro com 400 "arquivo inválido" — e o que já tinha sido
        // gravado antes da linha ruim ficava no banco
        MockMultipartFile file = posicaoAcoesComProdutoLongo();

        mockMvc.perform(multipart("/assets/import")
                        .file(file)
                        .header("Authorization", token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.created.length()").value(1))
                .andExpect(jsonPath("$.created[0].name").value("XPTO3 - XPTO S.A."))
                .andExpect(jsonPath("$.errors.length()").value(1))
                .andExpect(jsonPath("$.errors[0].isInformational").value(false));

        // a linha boa persistiu de verdade: 2 do fixture + 1 importada
        mockMvc.perform(get("/assets").header("Authorization", token))
                .andExpect(jsonPath("$.length()").value(3));
    }

    private MockMultipartFile posicaoAcoesComProdutoLongo() throws Exception {
        byte[] xlsx;
        try (XSSFWorkbook wb = new XSSFWorkbook()) {
            Sheet sheet = wb.createSheet("Acoes");
            Row header = sheet.createRow(0);
            header.createCell(0).setCellValue("Produto");
            header.createCell(1).setCellValue("Código de Negociação");
            header.createCell(2).setCellValue("Valor Atualizado");

            Row valida = sheet.createRow(1);
            valida.createCell(0).setCellValue("XPTO3 - XPTO S.A.");
            valida.createCell(1).setCellValue("XPTO3");
            valida.createCell(2).setCellValue(290.20);

            Row longa = sheet.createRow(2);
            longa.createCell(0).setCellValue("X".repeat(300));
            longa.createCell(1).setCellValue("XPTO4");
            longa.createCell(2).setCellValue(109.80);

            ByteArrayOutputStream out = new ByteArrayOutputStream();
            wb.write(out);
            xlsx = out.toByteArray();
        }
        return new MockMultipartFile("file", "posicao.xlsx", null, xlsx);
    }

    private MockMultipartFile posicaoRendaFixaFile() throws Exception {
        byte[] xlsx;
        try (XSSFWorkbook wb = new XSSFWorkbook()) {
            Sheet sheet = wb.createSheet("Renda Fixa");
            Row header = sheet.createRow(0);
            header.createCell(0).setCellValue("Produto");
            header.createCell(1).setCellValue("Código");
            header.createCell(2).setCellValue("Valor Atualizado CURVA");

            Row primeira = sheet.createRow(1);
            primeira.createCell(0).setCellValue("CDB - BANCO XPTO S.A.");
            primeira.createCell(1).setCellValue("CDB111AA11A");
            primeira.createCell(2).setCellValue(1000.00);

            Row segunda = sheet.createRow(2);
            segunda.createCell(0).setCellValue("CDB - BANCO XPTO S.A.");
            segunda.createCell(1).setCellValue("CDB222BB22B");
            segunda.createCell(2).setCellValue(2500.00);

            ByteArrayOutputStream out = new ByteArrayOutputStream();
            wb.write(out);
            xlsx = out.toByteArray();
        }
        return new MockMultipartFile("file", "posicao.xlsx", null, xlsx);
    }

    private MockMultipartFile posicaoAcoesFile(String produto, double valorAtualizado) throws Exception {
        byte[] xlsx;
        try (XSSFWorkbook wb = new XSSFWorkbook()) {
            Sheet sheet = wb.createSheet("Acoes");
            Row header = sheet.createRow(0);
            header.createCell(0).setCellValue("Produto");
            header.createCell(1).setCellValue("Código de Negociação");
            header.createCell(2).setCellValue("Valor Atualizado");
            Row data = sheet.createRow(1);
            data.createCell(0).setCellValue(produto);
            // o ticker é o primeiro token do Produto no export da B3 ("VALE3 - VALE S.A.")
            data.createCell(1).setCellValue(produto.split(" ")[0]);
            data.createCell(2).setCellValue(valorAtualizado);

            ByteArrayOutputStream out = new ByteArrayOutputStream();
            wb.write(out);
            xlsx = out.toByteArray();
        }
        return new MockMultipartFile("file", "posicao.xlsx", null, xlsx);
    }
}
