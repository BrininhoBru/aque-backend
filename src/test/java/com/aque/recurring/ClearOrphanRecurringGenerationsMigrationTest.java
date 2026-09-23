package com.aque.recurring;

import com.aque.BaseIntegrationTest;
import com.aque.category.Category;
import com.aque.category.CategoryRepository;
import com.aque.category.CategoryType;
import com.aque.transaction.Transaction;
import com.aque.transaction.TransactionRepository;
import com.aque.transaction.TransactionStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.util.FileCopyUtils;

import java.io.IOException;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;

// Executa o SQL da V8 de verdade (lido do classpath, não uma cópia) contra um banco com
// órfã e não-órfã plantadas. A migration já rodou no startup contra um schema vazio, então
// não há como observá-la fazendo efeito ali — e ela é um DELETE em dado de produção.
class ClearOrphanRecurringGenerationsMigrationTest extends BaseIntegrationTest {

    private static final String MIGRATION = "db/migration/V8__clear_orphan_recurring_generations.sql";

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private RecurringTransactionRepository recurringRepository;

    @Autowired
    private RecurringGenerationRepository recurringGenerationRepository;

    @Autowired
    private TransactionRepository transactionRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    private RecurringTransaction recurring;

    @BeforeEach
    void setup() {
        transactionRepository.deleteAll();
        recurringGenerationRepository.deleteAll();
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
    void migracaoV8_linhaSemTransacaoCorrespondente_eApagada() throws IOException {
        ledgerRow(3, 2026);

        runMigration();

        assertThat(recurringGenerationRepository.findAll()).isEmpty();
    }

    @Test
    void migracaoV8_linhaComTransacaoCorrespondente_eMantida() throws IOException {
        ledgerRow(3, 2026);
        transaction(3, 2026);

        runMigration();

        assertThat(recurringGenerationRepository.findAll()).hasSize(1);
    }

    // o caso que a migration não consegue distinguir de uma exclusão, documentado no
    // cabeçalho da V8: a instância foi movida pra abril, então a linha de março perde a
    // correspondência e é apagada junto
    @Test
    void migracaoV8_instanciaMovidaParaOutroMes_apagaALinhaDoMesOriginal() throws IOException {
        ledgerRow(3, 2026);
        transaction(4, 2026);

        runMigration();

        assertThat(recurringGenerationRepository.findAll()).isEmpty();
    }

    private void runMigration() throws IOException {
        String sql = FileCopyUtils.copyToString(new InputStreamReader(
                new ClassPathResource(MIGRATION).getInputStream(), StandardCharsets.UTF_8));
        jdbcTemplate.execute(sql);
    }

    private void ledgerRow(int month, int year) {
        RecurringGeneration generation = new RecurringGeneration();
        generation.setRecurringId(recurring.getId());
        generation.setReferenceMonth(month);
        generation.setReferenceYear(year);
        recurringGenerationRepository.save(generation);
    }

    private void transaction(int month, int year) {
        Transaction transaction = new Transaction();
        transaction.setDescription(recurring.getDescription());
        transaction.setCategory(recurring.getCategory());
        transaction.setType(recurring.getType());
        transaction.setReferenceMonth(month);
        transaction.setReferenceYear(year);
        transaction.setAmountExpected(recurring.getDefaultAmount());
        transaction.setStatus(TransactionStatus.PENDENTE);
        transaction.setRecurringId(recurring.getId());
        transaction.setOverride(false);
        transactionRepository.save(transaction);
    }
}
