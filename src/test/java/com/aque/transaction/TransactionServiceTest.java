package com.aque.transaction;

import com.aque.category.Category;
import com.aque.category.CategoryRepository;
import com.aque.category.CategoryType;
import com.aque.exception.BusinessException;
import com.aque.transaction.dto.request.PaymentUpdateRequest;
import com.aque.transaction.dto.request.TransactionRequest;
import com.aque.transaction.dto.response.TransactionResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TransactionServiceTest {

    @Mock
    private TransactionRepository transactionRepository;

    @Mock
    private CategoryRepository categoryRepository;

    @InjectMocks
    private TransactionService service;

    private Category category;

    @BeforeEach
    void setup() {
        category = new Category();
        category.setId(UUID.randomUUID());
        category.setName("Alimentação");
        category.setType(CategoryType.DESPESA);
    }

    @Test
    void create_semAmountPaid_ficaPendente() {
        var request = new TransactionRequest("Mercado", category.getId(), CategoryType.DESPESA,
                3, 2026, BigDecimal.valueOf(500), null, null);

        when(categoryRepository.findById(category.getId())).thenReturn(Optional.of(category));
        when(transactionRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        TransactionResponse response = service.create(request);

        assertThat(response.status()).isEqualTo(TransactionStatus.PENDENTE);
        assertThat(response.amountPaid()).isNull();
    }

    @Test
    void create_comAmountPaid_ficaPago() {
        var request = new TransactionRequest("Mercado", category.getId(), CategoryType.DESPESA,
                3, 2026, BigDecimal.valueOf(500), BigDecimal.valueOf(480), null);

        when(categoryRepository.findById(category.getId())).thenReturn(Optional.of(category));
        when(transactionRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        TransactionResponse response = service.create(request);

        assertThat(response.status()).isEqualTo(TransactionStatus.PAGO);
        assertThat(response.amountPaid()).isEqualByComparingTo("480");
    }

    @Test
    void create_typeDivergenteDaCategoria_lancaBusinessException400() {
        var request = new TransactionRequest("Salário", category.getId(), CategoryType.RECEITA,
                3, 2026, BigDecimal.valueOf(500), null, null);

        when(categoryRepository.findById(category.getId())).thenReturn(Optional.of(category));

        assertThatThrownBy(() -> service.create(request))
                .isInstanceOf(BusinessException.class)
                .extracting("status").isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    void create_categoriaNaoEncontrada_lancaBusinessException404() {
        var request = new TransactionRequest("Mercado", category.getId(), CategoryType.DESPESA,
                3, 2026, BigDecimal.valueOf(500), null, null);

        when(categoryRepository.findById(category.getId())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.create(request))
                .isInstanceOf(BusinessException.class)
                .extracting("status").isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void update_lancamentoOriginadoDeRecorrente_marcaOverride() {
        UUID id = UUID.randomUUID();
        Transaction existing = new Transaction();
        existing.setId(id);
        existing.setCategory(category);
        existing.setRecurringId(UUID.randomUUID());

        var request = new TransactionRequest("Mercado editado", category.getId(), CategoryType.DESPESA,
                3, 2026, BigDecimal.valueOf(600), null, null);

        when(transactionRepository.findById(id)).thenReturn(Optional.of(existing));
        when(categoryRepository.findById(category.getId())).thenReturn(Optional.of(category));
        when(transactionRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        TransactionResponse response = service.update(id, request);

        assertThat(response.isOverride()).isTrue();
    }

    @Test
    void update_typeDivergenteDaCategoria_lancaBusinessException400() {
        UUID id = UUID.randomUUID();
        Transaction existing = new Transaction();
        existing.setId(id);
        existing.setCategory(category);

        var request = new TransactionRequest("Mercado editado", category.getId(), CategoryType.RECEITA,
                3, 2026, BigDecimal.valueOf(600), null, null);

        when(transactionRepository.findById(id)).thenReturn(Optional.of(existing));
        when(categoryRepository.findById(category.getId())).thenReturn(Optional.of(category));

        assertThatThrownBy(() -> service.update(id, request))
                .isInstanceOf(BusinessException.class)
                .extracting("status").isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    void update_lancamentoNaoEncontrado_lancaBusinessException404() {
        UUID id = UUID.randomUUID();
        var request = new TransactionRequest("X", category.getId(), CategoryType.DESPESA,
                3, 2026, BigDecimal.valueOf(600), null, null);

        when(transactionRepository.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.update(id, request))
                .isInstanceOf(BusinessException.class)
                .extracting("status").isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void delete_lancamentoNaoEncontrado_lancaBusinessException404() {
        UUID id = UUID.randomUUID();
        when(transactionRepository.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.delete(id))
                .isInstanceOf(BusinessException.class)
                .extracting("status").isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void updatePayment_comValor_marcaPago() {
        UUID id = UUID.randomUUID();
        Transaction existing = new Transaction();
        existing.setId(id);
        existing.setCategory(category);
        existing.setDescription("Mercado");
        existing.setStatus(TransactionStatus.PENDENTE);

        var request = new PaymentUpdateRequest(BigDecimal.valueOf(480));

        when(transactionRepository.findById(id)).thenReturn(Optional.of(existing));
        when(transactionRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        TransactionResponse response = service.updatePayment(id, request);

        assertThat(response.status()).isEqualTo(TransactionStatus.PAGO);
        assertThat(response.amountPaid()).isEqualByComparingTo("480");
        assertThat(response.description()).isEqualTo("Mercado");
    }

    @Test
    void updatePayment_semValor_marcaPendente() {
        UUID id = UUID.randomUUID();
        Transaction existing = new Transaction();
        existing.setId(id);
        existing.setCategory(category);
        existing.setAmountPaid(BigDecimal.valueOf(480));
        existing.setStatus(TransactionStatus.PAGO);

        var request = new PaymentUpdateRequest(null);

        when(transactionRepository.findById(id)).thenReturn(Optional.of(existing));
        when(transactionRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        TransactionResponse response = service.updatePayment(id, request);

        assertThat(response.status()).isEqualTo(TransactionStatus.PENDENTE);
        assertThat(response.amountPaid()).isNull();
    }

    @Test
    void updatePayment_lancamentoDeRecorrente_marcaOverride() {
        UUID id = UUID.randomUUID();
        Transaction existing = new Transaction();
        existing.setId(id);
        existing.setCategory(category);
        existing.setRecurringId(UUID.randomUUID());

        var request = new PaymentUpdateRequest(BigDecimal.valueOf(100));

        when(transactionRepository.findById(id)).thenReturn(Optional.of(existing));
        when(transactionRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        TransactionResponse response = service.updatePayment(id, request);

        assertThat(response.isOverride()).isTrue();
    }

    @Test
    void updatePayment_lancamentoNaoEncontrado_lancaBusinessException404() {
        UUID id = UUID.randomUUID();
        var request = new PaymentUpdateRequest(BigDecimal.valueOf(100));

        when(transactionRepository.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.updatePayment(id, request))
                .isInstanceOf(BusinessException.class)
                .extracting("status").isEqualTo(HttpStatus.NOT_FOUND);
    }
}
