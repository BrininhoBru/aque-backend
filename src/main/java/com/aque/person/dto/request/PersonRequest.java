package com.aque.person.dto.request;

import jakarta.validation.constraints.NotBlank;

public record PersonRequest(
        @NotBlank(message = "Nome é obrigatório") String name
) {
}