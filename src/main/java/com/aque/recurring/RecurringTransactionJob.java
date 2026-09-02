package com.aque.recurring;

import com.aque.transaction.Transaction;
import com.aque.transaction.TransactionRepository;
import com.aque.transaction.TransactionStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class RecurringTransactionJob {

    private final RecurringTransactionRepository recurringRepository;
    private final TransactionRepository transactionRepository;
    private final RecurringGenerationRepository recurringGenerationRepository;

    @Scheduled(cron = "0 0 0 1 * *")
    @Transactional
    public void generateMonthlyTransactions() {
        LocalDate now = LocalDate.now();
        generate(now.getYear(), now.getMonthValue());
    }

    @Transactional
    public int generate(int year, int month) {
        log.info("Iniciando geração de recorrentes para {}/{}", month, year);

        List<RecurringTransaction> actives = recurringRepository.findByActive(true);
        int count = 0;

        for (RecurringTransaction recurring : actives) {
            try {
                boolean alreadyGenerated = recurringGenerationRepository
                        .existsByRecurringIdAndReferenceMonthAndReferenceYear(
                                recurring.getId(), month, year
                        );

                if (alreadyGenerated) {
                    log.debug("Recorrente {} já gerado para {}/{} — ignorando",
                            recurring.getId(), month, year);
                    continue;
                }

                Transaction transaction = getTransaction(year, month, recurring);

                transactionRepository.save(transaction);
                recurringGenerationRepository.save(generation(recurring.getId(), month, year));
                count++;
            } catch (RuntimeException e) {
                // isola a falha: uma recorrência com problema não pode travar a geração das demais
                log.error("Falha ao gerar instância do recorrente {} para {}/{}",
                        recurring.getId(), month, year, e);
            }
        }

        log.info("Geração concluída para {}/{}: {} instâncias criadas", month, year, count);
        return count;
    }

    private static Transaction getTransaction(int year, int month, RecurringTransaction recurring) {
        Transaction transaction = new Transaction();
        transaction.setDescription(recurring.getDescription());
        transaction.setCategory(recurring.getCategory());
        transaction.setType(recurring.getType());
        transaction.setReferenceMonth(month);
        transaction.setReferenceYear(year);
        transaction.setAmountExpected(recurring.getDefaultAmount());
        transaction.setAmountPaid(null);
        transaction.setStatus(TransactionStatus.PENDENTE);
        transaction.setRecurringId(recurring.getId());
        transaction.setOverride(false);
        transaction.setDueDate(dueDate(year, month, recurring.getDueDay()));
        return transaction;
    }

    // dueDay é opcional — sem ele, a instância gerada não tem vencimento (mesmo
    // comportamento de antes do dueDay existir). Quando presente, clampa pro último dia
    // real do mês (ex.: dueDay=31 em fevereiro vira 28 ou 29).
    private static LocalDate dueDate(int year, int month, Integer dueDay) {
        if (dueDay == null) {
            return null;
        }
        int lastDayOfMonth = YearMonth.of(year, month).lengthOfMonth();
        return LocalDate.of(year, month, Math.min(dueDay, lastDayOfMonth));
    }

    private static RecurringGeneration generation(UUID recurringId, int month, int year) {
        RecurringGeneration generation = new RecurringGeneration();
        generation.setRecurringId(recurringId);
        generation.setReferenceMonth(month);
        generation.setReferenceYear(year);
        return generation;
    }
}