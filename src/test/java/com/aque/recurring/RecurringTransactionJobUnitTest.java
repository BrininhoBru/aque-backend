package com.aque.recurring;

import com.aque.category.Category;
import com.aque.category.CategoryType;
import com.aque.transaction.TransactionRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RecurringTransactionJobUnitTest {

    @Mock
    private RecurringTransactionRepository recurringRepository;

    @Mock
    private TransactionRepository transactionRepository;

    @Mock
    private RecurringGenerationRepository recurringGenerationRepository;

    @InjectMocks
    private RecurringTransactionJob job;

    @Test
    void generate_recorrenteComFalha_naoBloqueiaAsDemais() {
        RecurringTransaction quebrado = recurring("Quebrado");
        RecurringTransaction ok = recurring("Aluguel");

        when(recurringRepository.findByActive(true)).thenReturn(List.of(quebrado, ok));
        when(recurringGenerationRepository.existsByRecurringIdAndReferenceMonthAndReferenceYear(any(), eq(3), eq(2026)))
                .thenReturn(false);
        when(transactionRepository.save(any()))
                .thenThrow(new RuntimeException("falha simulada"))
                .thenAnswer(inv -> inv.getArgument(0));

        int count = job.generate(2026, 3);

        assertThat(count).isEqualTo(1);
        verify(transactionRepository, times(2)).save(any());
    }

    @Test
    void generate_idempotente_ignoraRecorrenteJaGerado() {
        RecurringTransaction recurring = recurring("Aluguel");

        when(recurringRepository.findByActive(true)).thenReturn(List.of(recurring));
        when(recurringGenerationRepository.existsByRecurringIdAndReferenceMonthAndReferenceYear(
                recurring.getId(), 3, 2026)).thenReturn(true);

        int count = job.generate(2026, 3);

        assertThat(count).isZero();
        verify(transactionRepository, never()).save(any());
    }

    private RecurringTransaction recurring(String description) {
        Category category = new Category();
        category.setId(UUID.randomUUID());
        category.setType(CategoryType.DESPESA);

        RecurringTransaction recurring = new RecurringTransaction();
        recurring.setId(UUID.randomUUID());
        recurring.setDescription(description);
        recurring.setCategory(category);
        recurring.setType(CategoryType.DESPESA);
        recurring.setDefaultAmount(BigDecimal.valueOf(1000));
        recurring.setActive(true);
        return recurring;
    }
}
