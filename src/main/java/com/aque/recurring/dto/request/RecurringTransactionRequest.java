package com.aque.recurring.dto.request;

import com.aque.category.CategoryType;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.util.UUID;

public record RecurringTransactionRequest(
        @NotBlank(message = "Descrição é obrigatória")
        String description,

        @NotNull(message = "Categoria é obrigatória")
        UUID categoryId,

        @NotNull(message = "Tipo é obrigatório")
        CategoryType type,

        @NotNull(message = "Valor padrão é obrigatório")
        @DecimalMin(value = "0.0", inclusive = false, message = "Valor padrão deve ser positivo")
        BigDecimal defaultAmount
) {
}