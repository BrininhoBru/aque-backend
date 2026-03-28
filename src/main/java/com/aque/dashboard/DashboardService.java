package com.aque.dashboard;

import com.aque.category.Category;
import com.aque.category.CategoryType;
import com.aque.category.dto.response.CategoryResponse;
import com.aque.dashboard.dto.response.CategoryTotalResponse;
import com.aque.dashboard.dto.response.DashboardSummaryResponse;
import com.aque.dashboard.dto.response.MonthEvolutionResponse;
import com.aque.dashboard.dto.response.SplitResultResponse;
import com.aque.exception.BusinessException;
import com.aque.person.dto.response.PersonResponse;
import com.aque.split.SplitRule;
import com.aque.split.SplitRuleRepository;
import com.aque.transaction.TransactionRepository;
import com.aque.transaction.TransactionStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class DashboardService {

    private final TransactionRepository transactionRepository;
    private final SplitRuleRepository splitRuleRepository;

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

    public List<CategoryTotalResponse> getByCategory(int year, int month, CategoryType type) {
        List<Object[]> rows = transactionRepository.sumByCategory(month, year, type);

        return rows.stream()
                .map(row -> new CategoryTotalResponse(
                        CategoryResponse.from((Category) row[0]),
                        (BigDecimal) row[1],
                        (BigDecimal) row[2]
                ))
                .toList();
    }

    public List<MonthEvolutionResponse> getEvolution(int year) {
        List<Object[]> rows = transactionRepository.evolutionByYear(year);

        Map<Integer, Object[]> dataByMonth = rows.stream()
                .collect(Collectors.toMap(row -> (Integer) row[0], row -> row));

        List<MonthEvolutionResponse> result = new ArrayList<>();
        for (int m = 1; m <= 12; m++) {
            Object[] row = dataByMonth.get(m);
            if (row != null) {
                result.add(new MonthEvolutionResponse(m,
                        (BigDecimal) row[1], (BigDecimal) row[2],
                        (BigDecimal) row[3], (BigDecimal) row[4]));
            } else {
                result.add(new MonthEvolutionResponse(m,
                        BigDecimal.ZERO, BigDecimal.ZERO,
                        BigDecimal.ZERO, BigDecimal.ZERO));
            }
        }
        return result;
    }

    public SplitResultResponse getSplit(int year, int month) {
        SplitRule rule = splitRuleRepository
                .findByReferenceMonthAndReferenceYear(month, year)
                .orElseThrow(() -> new BusinessException(
                        "Regra de divisão não configurada para " + month + "/" + year,
                        HttpStatus.NOT_FOUND
                ));

        BigDecimal totalExpense = transactionRepository
                .sumExpected(month, year, CategoryType.DESPESA);

        List<SplitResultResponse.SplitResultItemResponse> items = rule.getItems().stream()
                .map(item -> {
                    BigDecimal amount = totalExpense
                            .multiply(item.getPercentage())
                            .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
                    return new SplitResultResponse.SplitResultItemResponse(
                            PersonResponse.from(item.getPerson()),
                            item.getPercentage(),
                            amount
                    );
                })
                .toList();

        return new SplitResultResponse(totalExpense, items);
    }
}