package com.aque.dashboard.dto.response;

import java.math.BigDecimal;

public record DashboardSummaryResponse(
        BigDecimal totalIncomeExpected,
        BigDecimal totalIncomePaid,
        BigDecimal totalExpenseExpected,
        BigDecimal totalExpensePaid,
        BigDecimal balanceExpected,
        BigDecimal balancePaid
) {
}