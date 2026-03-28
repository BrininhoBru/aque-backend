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
import java.util.List;
import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class RecurringTransactionJob {

    private final RecurringTransactionRepository recurringRepository;
    private final TransactionRepository transactionRepository;

    @Scheduled(cron = "0 0 0 1 * *")
    @Transactional
    public void generateMonthlyTransactions() {
        LocalDate now = LocalDate.now();
        generate(now.getYear(), now.getMonthValue());
    }

    @Transactional
    public int generate(int year, int month) {
        log.info("Iniciando geração de recorrentes para {}/{}", month, year);

        List<RecurringTransaction> actives = recurringRepository.findByActiveTrue();
        int count = 0;

        for (RecurringTransaction recurring : actives) {
            boolean alreadyExists = transactionRepository
                    .existsByRecurringIdAndReferenceMonthAndReferenceYear(
                            recurring.getId(), month, year
                    );

            if (alreadyExists) {
                log.debug("Recorrente {} já gerado para {}/{} — ignorando",
                        recurring.getId(), month, year);
                continue;
            }

            Transaction transaction = getTransaction(year, month, recurring);

            transactionRepository.save(transaction);
            count++;
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
        return transaction;
    }
}