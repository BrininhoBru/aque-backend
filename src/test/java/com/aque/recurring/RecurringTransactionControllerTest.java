package com.aque.recurring;

import com.aque.BaseIntegrationTest;
import com.aque.category.Category;
import com.aque.category.CategoryRepository;
import com.aque.category.CategoryType;
import com.aque.recurring.dto.request.RecurringTransactionRequest;
import com.aque.transaction.TransactionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@AutoConfigureMockMvc
class RecurringTransactionControllerTest extends BaseIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private RecurringTransactionRepository recurringRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private TransactionRepository transactionRepository;

    @Autowired
    private RecurringGenerationRepository recurringGenerationRepository;

    private Category category;
    private RecurringTransaction recurring;

    @BeforeEach
    void setupRecurring() {
        // /recurring/generate/{year}/{month} cria Transaction e RecurringGeneration
        // reais ligadas ao recorrente via FK — precisa limpar antes de apagar
        // recurring_transactions
        transactionRepository.deleteAll();
        recurringGenerationRepository.deleteAll();
        recurringRepository.deleteAll();
        categoryRepository.deleteAll();

        category = new Category();
        category.setName("Moradia");
        category.setType(CategoryType.DESPESA);
        categoryRepository.save(category);

        recurring = new RecurringTransaction();
        recurring.setDescription("Aluguel");
        recurring.setCategory(category);
        recurring.setType(CategoryType.DESPESA);
        recurring.setDefaultAmount(BigDecimal.valueOf(1500));
        recurring.setActive(true);
        recurringRepository.save(recurring);
    }

    @Test
    void listarRecorrentes_semFiltro_retornaTodos() throws Exception {
        mockMvc.perform(get("/recurring")
                        .header("Authorization", token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1));
    }

    @Test
    void listarRecorrentes_filtrandoPorAtivo_retornaFiltrados() throws Exception {
        mockMvc.perform(get("/recurring?active=false")
                        .header("Authorization", token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    void criarRecorrente_deveRetornar201() throws Exception {
        String body = objectMapper.writeValueAsString(
                new RecurringTransactionRequest("Internet", category.getId(), CategoryType.DESPESA, BigDecimal.valueOf(100), null));

        mockMvc.perform(post("/recurring")
                        .header("Authorization", token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.description").value("Internet"))
                .andExpect(jsonPath("$.active").value(true));
    }

    @Test
    void criarRecorrente_categoriaInexistente_deveRetornar404() throws Exception {
        String body = objectMapper.writeValueAsString(
                new RecurringTransactionRequest("Internet", UUID.randomUUID(), CategoryType.DESPESA, BigDecimal.valueOf(100), null));

        mockMvc.perform(post("/recurring")
                        .header("Authorization", token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isNotFound());
    }

    @Test
    void editarRecorrente_deveRetornar200() throws Exception {
        String body = objectMapper.writeValueAsString(
                new RecurringTransactionRequest("Aluguel Novo", category.getId(), CategoryType.DESPESA, BigDecimal.valueOf(1600), null));

        mockMvc.perform(put("/recurring/" + recurring.getId())
                        .header("Authorization", token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.description").value("Aluguel Novo"));
    }

    @Test
    void desativarRecorrente_deveRetornar204() throws Exception {
        mockMvc.perform(delete("/recurring/" + recurring.getId())
                        .header("Authorization", token))
                .andExpect(status().isNoContent());
    }

    @Test
    void desativarRecorrenteInexistente_deveRetornar404() throws Exception {
        mockMvc.perform(delete("/recurring/00000000-0000-0000-0000-000000000000")
                        .header("Authorization", token))
                .andExpect(status().isNotFound());
    }

    @Test
    void gerarInstancias_deveRetornar200() throws Exception {
        mockMvc.perform(post("/recurring/generate/2026/3")
                        .header("Authorization", token))
                .andExpect(status().isOk());
    }
}
