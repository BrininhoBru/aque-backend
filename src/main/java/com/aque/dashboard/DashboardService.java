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
import com.aque.split.SplitRuleItem;
import com.aque.split.SplitRuleRepository;
import com.aque.transaction.TransactionRepository;
import com.aque.transaction.TransactionStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
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

        BigDecimal overdueAmount = transactionRepository.sumOverdue(CategoryType.DESPESA);
        long overdueCount = transactionRepository.countOverdue(CategoryType.DESPESA);

        return new DashboardSummaryResponse(
                incomeExpected,
                incomePaid,
                expenseExpected,
                expensePaid,
                incomeExpected.subtract(expenseExpected),
                incomePaid.subtract(expensePaid),
                incomeExpected.subtract(incomePaid),
                expenseExpected.subtract(expensePaid),
                overdueAmount,
                overdueCount
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
                .findTopByEffectiveFromLessThanEqualOrderByEffectiveFromDescCreatedAtDesc(LocalDate.of(year, month, 1))
                .orElseThrow(() -> new BusinessException(
                        "Regra de divisão não configurada para " + month + "/" + year,
                        HttpStatus.NOT_FOUND
                ));

        BigDecimal totalExpense = transactionRepository
                .sumExpected(month, year, CategoryType.DESPESA);

        // Último item recebe o resto da divisão em vez de arredondar independente,
        // senão a soma dos itens pode não bater com totalExpense por causa do HALF_UP.
        List<SplitRuleItem> ruleItems = new ArrayList<>(rule.getItems());
        ruleItems.sort(java.util.Comparator.comparing(i -> i.getPerson().getId()));
        List<SplitResultResponse.SplitResultItemResponse> items = new ArrayList<>();
        BigDecimal allocated = BigDecimal.ZERO;
        for (int i = 0; i < ruleItems.size(); i++) {
            SplitRuleItem item = ruleItems.get(i);
            boolean isLast = i == ruleItems.size() - 1;
            BigDecimal amount = isLast
                    ? totalExpense.subtract(allocated)
                    : totalExpense.multiply(item.getPercentage())
                            .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
            if (!isLast) {
                allocated = allocated.add(amount);
            }
            items.add(new SplitResultResponse.SplitResultItemResponse(
                    PersonResponse.from(item.getPerson()),
                    item.getPercentage(),
                    amount
            ));
        }

        return new SplitResultResponse(totalExpense, items);
    }
}