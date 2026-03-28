package com.aque.recurring.dto.response;

import com.aque.category.CategoryType;
import com.aque.category.dto.response.CategoryResponse;
import com.aque.recurring.RecurringTransaction;

import java.math.BigDecimal;
import java.util.UUID;

public record RecurringTransactionResponse(
        UUID id,
        String description,
        CategoryResponse category,
        CategoryType type,
        BigDecimal defaultAmount,
        boolean active
) {
    public static RecurringTransactionResponse from(RecurringTransaction recurring) {
        return new RecurringTransactionResponse(
                recurring.getId(),
                recurring.getDescription(),
                CategoryResponse.from(recurring.getCategory()),
                recurring.getType(),
                recurring.getDefaultAmount(),
                recurring.isActive()
        );
    }
}