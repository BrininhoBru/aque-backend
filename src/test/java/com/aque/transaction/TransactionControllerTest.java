package com.aque.transaction;

import com.aque.BaseIntegrationTest;
import com.aque.category.Category;
import com.aque.category.CategoryRepository;
import com.aque.category.CategoryType;
import com.aque.recurring.RecurringTransactionRepository;
import com.aque.transaction.dto.request.PaymentUpdateRequest;
import com.aque.transaction.dto.request.TransactionRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@AutoConfigureMockMvc
class TransactionControllerTest extends BaseIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private TransactionRepository transactionRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    private Category category;

    @Autowired
    private RecurringTransactionRepository recurringRepository;

    @BeforeEach
    void setup() {
        transactionRepository.deleteAll();
        recurringRepository.deleteAll();
        categoryRepository.deleteAll();

        category = new Category();
        category.setName("Alimentação");
        category.setType(CategoryType.DESPESA);
        category.setPredefined(false);
        categoryRepository.save(category);
    }

    @Test
    void criarLancamento_semValorPago_deveSerPendente() throws Exception {
        var request = new TransactionRequest(
                "Supermercado", category.getId(), CategoryType.DESPESA,
                3, 2026, BigDecimal.valueOf(500), null, null);

        mockMvc.perform(post("/transactions")
                        .header("Authorization", token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("PENDENTE"));
    }

    @Test
    void criarLancamento_comValorPago_deveSerPago() throws Exception {
        var request = new TransactionRequest(
                "Supermercado", category.getId(), CategoryType.DESPESA,
                3, 2026, BigDecimal.valueOf(500), BigDecimal.valueOf(480), null);

        mockMvc.perform(post("/transactions")
                        .header("Authorization", token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("PAGO"))
                .andExpect(jsonPath("$.amountPaid").value(480));
    }

    @Test
    void valorNegativo_deveRetornar400() throws Exception {
        var request = new TransactionRequest(
                "Inválido", category.getId(), CategoryType.DESPESA,
                3, 2026, BigDecimal.valueOf(-100), null, null);

        mockMvc.perform(post("/transactions")
                        .header("Authorization", token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void mesInvalido_deveRetornar400() throws Exception {
        var request = new TransactionRequest(
                "Inválido", category.getId(), CategoryType.DESPESA,
                13, 2026, BigDecimal.valueOf(100), null, null);

        mockMvc.perform(post("/transactions")
                        .header("Authorization", token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void marcarPagamento_comValor_atualizaSoStatusEValorPago() throws Exception {
        var createRequest = new TransactionRequest(
                "Supermercado", category.getId(), CategoryType.DESPESA,
                3, 2026, BigDecimal.valueOf(500), null, null);

        String createBody = mockMvc.perform(post("/transactions")
                        .header("Authorization", token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createRequest)))
                .andReturn().getResponse().getContentAsString();
        String id = objectMapper.readTree(createBody).get("id").asText();

        var paymentRequest = new PaymentUpdateRequest(BigDecimal.valueOf(480));

        mockMvc.perform(patch("/transactions/{id}/payment", id)
                        .header("Authorization", token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(paymentRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("PAGO"))
                .andExpect(jsonPath("$.amountPaid").value(480))
                .andExpect(jsonPath("$.description").value("Supermercado"))
                .andExpect(jsonPath("$.referenceMonth").value(3));
    }

    @Test
    void marcarPagamento_lancamentoNaoEncontrado_deveRetornar404() throws Exception {
        var paymentRequest = new PaymentUpdateRequest(BigDecimal.valueOf(100));

        mockMvc.perform(patch("/transactions/{id}/payment", java.util.UUID.randomUUID())
                        .header("Authorization", token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(paymentRequest)))
                .andExpect(status().isNotFound());
    }

    @Test
    void filtrarPorMesEAno_deveRetornarApenasMesFiltrado() throws Exception {
        var request = new TransactionRequest(
                "Supermercado", category.getId(), CategoryType.DESPESA,
                3, 2026, BigDecimal.valueOf(500), null, null);

        mockMvc.perform(post("/transactions")
                .header("Authorization", token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)));

        mockMvc.perform(get("/transactions?month=3&year=2026")
                        .header("Authorization", token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1));

        mockMvc.perform(get("/transactions?month=4&year=2026")
                        .header("Authorization", token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }
}