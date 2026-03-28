package com.aque.transaction.dto.request;

import com.aque.category.CategoryType;
import jakarta.validation.constraints.*;

import java.math.BigDecimal;
import java.util.UUID;

public record TransactionRequest(
        @NotBlank(message = "Descrição é obrigatória")
        String description,

        @NotNull(message = "Categoria é obrigatória")
        UUID categoryId,

        @NotNull(message = "Tipo é obrigatório")
        CategoryType type,

        @NotNull(message = "Mês de referência é obrigatório")
        @Min(value = 1, message = "Mês deve ser entre 1 e 12")
        @Max(value = 12, message = "Mês deve ser entre 1 e 12")
        Integer referenceMonth,

        @NotNull(message = "Ano de referência é obrigatório")
        Integer referenceYear,

        @NotNull(message = "Valor previsto é obrigatório")
        @DecimalMin(value = "0.0", inclusive = false, message = "Valor previsto deve ser positivo")
        BigDecimal amountExpected,

        @DecimalMin(value = "0.0", inclusive = false, message = "Valor pago deve ser positivo")
        BigDecimal amountPaid
) {
}