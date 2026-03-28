package com.aque.split.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;

import java.util.List;

public record SplitRuleRequest(
        @NotEmpty(message = "A lista de itens não pode ser vazia")
        @Valid
        List<SplitRuleItemRequest> items
) {
}