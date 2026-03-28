package com.aque.category.dto.request;

import com.aque.category.CategoryType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record CategoryRequest(
        @NotBlank(message = "Nome é obrigatório") String name,
        @NotNull(message = "Tipo é obrigatório") CategoryType type
) {
}