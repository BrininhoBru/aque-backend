package com.aque.recurring;

import com.aque.BaseIntegrationTest;
import com.aque.category.Category;
import com.aque.category.CategoryRepository;
import com.aque.category.CategoryType;
import com.aque.transaction.Transaction;
import com.aque.transaction.TransactionRepository;
import com.aque.transaction.TransactionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

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

    @Autowired
    private TransactionService transactionService;

    @Autowired
    private RecurringGenerationRepository recurringGenerationRepository;

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

    @Test
    void generate_moverInstanciaParaOutroMes_naoDuplicaAoGerarDeNovo() {
        job.generate(2026, 3);
        Transaction generated = transactionRepository.findAll().getFirst();

        // move direto pelo repositório (não pelo TransactionService.update()) — o que
        // este teste verifica é a idempotência do job diante de uma transação movida de
        // mês, não o fluxo de edição em si
        generated.setReferenceMonth(4);
        transactionRepository.save(generated);

        int count = job.generate(2026, 3);

        assertThat(count).isZero();
        assertThat(transactionRepository.findAll()).hasSize(1);
    }

    // inverte o contrato que a #19 tinha estabelecido: naquela época excluir a instância
    // era definitivo, porque o ledger era a única fonte de verdade sobre "já gerou". Na
    // prática isso deixava o mês marcado como gerado sem instância nenhuma, e o recorrente
    // era ignorado pra sempre (#36)
    @Test
    void generate_apagarInstanciaGerada_recriaAoGerarDeNovo() {
        job.generate(2026, 3);
        Transaction generated = transactionRepository.findAll().getFirst();
        transactionService.delete(generated.getId());

        int count = job.generate(2026, 3);

        assertThat(count).isEqualTo(1);
        assertThat(transactionRepository.findAll()).hasSize(1);
    }

    // compara o id da linha do ledger antes e depois: só asserir "tem uma linha" passaria
    // também no código sem o fix, onde a regeração simplesmente não acontece
    @Test
    void generate_apagarInstanciaGerada_trocaRegistroDeGeracaoSemDuplicar() {
        job.generate(2026, 3);
        UUID ledgerIdBefore = recurringGenerationRepository.findAll().getFirst().getId();
        transactionService.delete(transactionRepository.findAll().getFirst().getId());

        job.generate(2026, 3);

        assertThat(recurringGenerationRepository.findAll())
                .singleElement()
                .extracting(RecurringGeneration::getId)
                .isNotEqualTo(ledgerIdBefore);
    }

    @Test
    void generate_recorrenteComDueDay_geraTransacaoComVencimentoNoMesDeReferencia() {
        recurring.setDueDay(5);
        recurringRepository.save(recurring);

        job.generate(2026, 3);

        Transaction generated = transactionRepository.findAll().getFirst();
        assertThat(generated.getDueDate()).isEqualTo(LocalDate.of(2026, 3, 5));
    }

    @Test
    void generate_recorrenteComDueDayMaiorQueUltimoDiaDoMes_clampiaParaOUltimoDia() {
        recurring.setDueDay(31);
        recurringRepository.save(recurring);

        job.generate(2026, 2); // fevereiro/2026 tem 28 dias

        Transaction generated = transactionRepository.findAll().getFirst();
        assertThat(generated.getDueDate()).isEqualTo(LocalDate.of(2026, 2, 28));
    }

    @Test
    void generate_recorrenteSemDueDay_geraTransacaoSemVencimento() {
        job.generate(2026, 3);

        Transaction generated = transactionRepository.findAll().getFirst();
        assertThat(generated.getDueDate()).isNull();
    }
}