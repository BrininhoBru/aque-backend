package com.aque.split.dto.response;

import com.aque.person.dto.response.PersonResponse;
import com.aque.split.SplitRule;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public record SplitRuleResponse(UUID id, Integer referenceMonth, Integer referenceYear,
                                List<SplitRuleItemResponse> items) {
    public record SplitRuleItemResponse(
            PersonResponse person,
            BigDecimal percentage
    ) {
    }

    public static SplitRuleResponse from(SplitRule rule) {
        return new SplitRuleResponse(
                rule.getId(),
                rule.getReferenceMonth(),
                rule.getReferenceYear(),
                rule.getItems().stream()
                        .map(item -> new SplitRuleItemResponse(
                                PersonResponse.from(item.getPerson()),
                                item.getPercentage()
                        ))
                        .toList()
        );
    }
}