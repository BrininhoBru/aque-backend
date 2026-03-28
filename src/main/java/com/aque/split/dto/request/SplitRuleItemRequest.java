package com.aque.split.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.util.UUID;

public record SplitRuleItemRequest(
        @NotNull(message = "Pessoa é obrigatória")
        UUID personId,

        @NotNull(message = "Percentual é obrigatório")
        @DecimalMin(value = "0.0", inclusive = false, message = "Percentual deve ser maior que zero")
        BigDecimal percentage
) {
}