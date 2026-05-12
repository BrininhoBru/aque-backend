package com.aque.transaction.dto.response;

import com.aque.category.CategoryType;
import com.aque.category.dto.response.CategoryResponse;
import com.aque.transaction.Transaction;
import com.aque.transaction.TransactionStatus;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public record TransactionResponse(
        UUID id,
        String description,
        CategoryResponse category,
        CategoryType type,
        Integer referenceMonth,
        Integer referenceYear,
        BigDecimal amountExpected,
        BigDecimal amountPaid,
        TransactionStatus status,
        LocalDate dueDate,
        UUID recurringId,
        boolean isOverride
) {
    public static TransactionResponse from(Transaction transaction) {
        return new TransactionResponse(
                transaction.getId(),
                transaction.getDescription(),
                CategoryResponse.from(transaction.getCategory()),
                transaction.getType(),
                transaction.getReferenceMonth(),
                transaction.getReferenceYear(),
                transaction.getAmountExpected(),
                transaction.getAmountPaid(),
                transaction.getStatus(),
                transaction.getDueDate(),
                transaction.getRecurringId(),
                transaction.isOverride()
        );
    }
}