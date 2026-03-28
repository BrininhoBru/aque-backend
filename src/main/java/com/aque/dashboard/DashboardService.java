package com.aque.dashboard;

import com.aque.category.CategoryType;
import com.aque.dashboard.dto.response.DashboardSummaryResponse;
import com.aque.transaction.TransactionRepository;
import com.aque.transaction.TransactionStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

@Service
@RequiredArgsConstructor
public class DashboardService {

    private final TransactionRepository transactionRepository;

    public DashboardSummaryResponse getSummary(int year, int month) {
        BigDecimal incomeExpected = transactionRepository
                .sumExpected(month, year, CategoryType.RECEITA);

        BigDecimal incomePaid = transactionRepository
                .sumPaid(month, year, CategoryType.RECEITA, TransactionStatus.PAGO);

        BigDecimal expenseExpected = transactionRepository
                .sumExpected(month, year, CategoryType.DESPESA);

        BigDecimal expensePaid = transactionRepository
                .sumPaid(month, year, CategoryType.DESPESA, TransactionStatus.PAGO);

        return new DashboardSummaryResponse(
                incomeExpected,
                incomePaid,
                expenseExpected,
                expensePaid,
                incomeExpected.subtract(expenseExpected),
                incomePaid.subtract(expensePaid)
        );
    }
}