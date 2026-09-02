package com.aque.transaction.dto.request;

import jakarta.validation.constraints.DecimalMin;

import java.math.BigDecimal;

public record PaymentUpdateRequest(
        @DecimalMin(value = "0.0", inclusive = false, message = "Valor pago deve ser positivo")
        BigDecimal amountPaid
) {
}
