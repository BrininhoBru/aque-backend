package com.aque.asset.dto.request;

import com.aque.asset.AssetType;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.util.UUID;

public record AssetRequest(
        @NotBlank(message = "Nome é obrigatório") String name,
        @NotNull(message = "Tipo é obrigatório") AssetType type,
        @NotNull(message = "Valor atual é obrigatório")
        @DecimalMin(value = "0.0", message = "Valor atual não pode ser negativo") BigDecimal currentValue,
        UUID personId
) {
}
