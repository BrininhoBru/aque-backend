package com.aque.dashboard;

import com.aque.category.Category;
import com.aque.category.CategoryType;
import com.aque.dashboard.dto.response.DashboardSummaryResponse;
import com.aque.dashboard.dto.response.MonthEvolutionResponse;
import com.aque.dashboard.dto.response.SplitResultResponse;
import com.aque.exception.BusinessException;
import com.aque.person.Person;
import com.aque.split.SplitRule;
import com.aque.split.SplitRuleItem;
import com.aque.split.SplitRuleRepository;
import com.aque.transaction.TransactionRepository;
import com.aque.transaction.TransactionStatus;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DashboardServiceTest {

    @Mock
    private TransactionRepository transactionRepository;

    @Mock
    private SplitRuleRepository splitRuleRepository;

    @InjectMocks
    private DashboardService service;

    @Test
    void getSummary_calculaSaldoPrevistoEPago() {
        when(transactionRepository.sumExpected(3, 2026, CategoryType.RECEITA))
                .thenReturn(BigDecimal.valueOf(5000));
        when(transactionRepository.sumPaid(3, 2026, CategoryType.RECEITA, TransactionStatus.PAGO))
                .thenReturn(BigDecimal.valueOf(4000));
        when(transactionRepository.sumExpected(3, 2026, CategoryType.DESPESA))
                .thenReturn(BigDecimal.valueOf(3000));
        when(transactionRepository.sumPaid(3, 2026, CategoryType.DESPESA, TransactionStatus.PAGO))
                .thenReturn(BigDecimal.valueOf(1000));
        when(transactionRepository.sumOverdue(CategoryType.DESPESA))
                .thenReturn(BigDecimal.valueOf(200));
        when(transactionRepository.countOverdue(CategoryType.DESPESA))
                .thenReturn(2L);

        DashboardSummaryResponse summary = service.getSummary(2026, 3);

        assertThat(summary.balanceExpected()).isEqualByComparingTo("2000");
        assertThat(summary.balancePaid()).isEqualByComparingTo("3000");
        assertThat(summary.totalIncomePending()).isEqualByComparingTo("1000");
        assertThat(summary.totalExpensePending()).isEqualByComparingTo("2000");
        assertThat(summary.totalOverdueAmount()).isEqualByComparingTo("200");
        assertThat(summary.totalOverdueCount()).isEqualTo(2L);
    }

    @Test
    void getEvolution_preenchePeriodosSemLancamentoComZero() {
        Object[] marchRow = {3, BigDecimal.valueOf(1000), BigDecimal.valueOf(900),
                BigDecimal.valueOf(500), BigDecimal.valueOf(500)};
        when(transactionRepository.evolutionByYear(2026)).thenReturn(java.util.Collections.singletonList(marchRow));

        List<MonthEvolutionResponse> evolution = service.getEvolution(2026);

        assertThat(evolution).hasSize(12);
        assertThat(evolution.get(2).month()).isEqualTo(3);
        assertThat(evolution.get(2).totalIncomeExpected()).isEqualByComparingTo("1000");
        assertThat(evolution.get(0).month()).isEqualTo(1);
        assertThat(evolution.get(0).totalIncomeExpected()).isEqualByComparingTo("0");
    }

    @Test
    void getSplit_regraNaoConfigurada_lancaBusinessException404() {
        when(splitRuleRepository.findByReferenceMonthAndReferenceYear(4, 2026))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getSplit(2026, 4))
                .isInstanceOf(BusinessException.class)
                .extracting("status").isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void getSplit_ultimoItemAbsorveORestoDoArredondamento() {
        SplitRule rule = new SplitRule();
        rule.setReferenceMonth(3);
        rule.setReferenceYear(2026);
        rule.getItems().add(item(rule, "A", "33.34"));
        rule.getItems().add(item(rule, "B", "33.33"));
        rule.getItems().add(item(rule, "C", "33.33"));

        when(splitRuleRepository.findByReferenceMonthAndReferenceYear(3, 2026))
                .thenReturn(Optional.of(rule));
        when(transactionRepository.sumExpected(3, 2026, CategoryType.DESPESA))
                .thenReturn(new BigDecimal("100.01"));

        SplitResultResponse result = service.getSplit(2026, 3);

        BigDecimal sum = result.items().stream()
                .map(SplitResultResponse.SplitResultItemResponse::amount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        assertThat(sum).isEqualByComparingTo(result.totalExpenseExpected());
        // primeiros dois arredondam normal, o terceiro (último) fica com o centavo de resto
        assertThat(result.items().get(0).amount()).isEqualByComparingTo("33.34");
        assertThat(result.items().get(1).amount()).isEqualByComparingTo("33.33");
        assertThat(result.items().get(2).amount()).isEqualByComparingTo("33.34");
    }

    private SplitRuleItem item(SplitRule rule, String personName, String percentage) {
        Person person = new Person();
        person.setName(personName);
        SplitRuleItem item = new SplitRuleItem();
        item.setSplitRule(rule);
        item.setPerson(person);
        item.setPercentage(new BigDecimal(percentage));
        return item;
    }
}
