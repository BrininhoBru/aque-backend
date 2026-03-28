package com.aque.dashboard.dto.response;

import com.aque.person.dto.response.PersonResponse;

import java.math.BigDecimal;
import java.util.List;

public record SplitResultResponse(
        BigDecimal totalExpenseExpected,
        List<SplitResultItemResponse> items
) {
    public record SplitResultItemResponse(
            PersonResponse person,
            BigDecimal percentage,
            BigDecimal amount
    ) {}
}