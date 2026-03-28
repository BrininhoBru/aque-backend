package com.aque.recurring;

import com.aque.BaseIntegrationTest;
import com.aque.category.Category;
import com.aque.category.CategoryRepository;
import com.aque.category.CategoryType;
import com.aque.transaction.TransactionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

class RecurringTransactionJobTest extends BaseIntegrationTest {

    @Autowired
    private RecurringTransactionJob job;

    @Autowired
    private RecurringTransactionRepository recurringRepository;

    @Autowired
    private TransactionRepository transactionRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    private RecurringTransaction recurring;

    @BeforeEach
    void setup() {
        transactionRepository.deleteAll();
        recurringRepository.deleteAll();
        categoryRepository.deleteAll();

        Category category = new Category();
        category.setName("Aluguel");
        category.setType(CategoryType.DESPESA);
        category.setPredefined(false);
        categoryRepository.save(category);

        recurring = new RecurringTransaction();
        recurring.setDescription("Aluguel mensal");
        recurring.setCategory(category);
        recurring.setType(CategoryType.DESPESA);
        recurring.setDefaultAmount(BigDecimal.valueOf(1500));
        recurring.setActive(true);
        recurringRepository.save(recurring);
    }

    @Test
    void generate_deveGerarInstanciaParaOmes() {
        int count = job.generate(2026, 3);
        assertThat(count).isEqualTo(1);
        assertThat(transactionRepository.findAll()).hasSize(1);
    }

    @Test
    void generate_deveSerIdempotente() {
        job.generate(2026, 3);
        int count = job.generate(2026, 3);
        assertThat(count).isEqualTo(0);
        assertThat(transactionRepository.findAll()).hasSize(1);
    }

    @Test
    void generate_recorrenteInativo_naoDeveGerar() {
        recurring.setActive(false);
        recurringRepository.save(recurring);

        int count = job.generate(2026, 3);
        assertThat(count).isEqualTo(0);
        assertThat(transactionRepository.findAll()).isEmpty();
    }
}