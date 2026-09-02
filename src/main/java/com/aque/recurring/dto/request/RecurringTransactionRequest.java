package com.aque.recurring.dto.request;

import com.aque.category.CategoryType;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
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
        BigDecimal defaultAmount,

        // opcional — sem esse campo, as instâncias geradas continuam sem dueDate
        @Min(value = 1, message = "Dia do vencimento deve ser entre 1 e 31")
        @Max(value = 31, message = "Dia do vencimento deve ser entre 1 e 31")
        Integer dueDay
) {
}