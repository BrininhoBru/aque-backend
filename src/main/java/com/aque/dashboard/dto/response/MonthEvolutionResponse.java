package com.aque.dashboard.dto.response;

import java.math.BigDecimal;

public record MonthEvolutionResponse(
        Integer month,
        BigDecimal totalIncomeExpected,
        BigDecimal totalIncomePaid,
        BigDecimal totalExpenseExpected,
        BigDecimal totalExpensePaid
) {}