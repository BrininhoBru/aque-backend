package com.aque.category.dto.response;

import com.aque.category.Category;
import com.aque.category.CategoryType;

import java.util.UUID;

public record CategoryResponse(
        UUID id,
        String name,
        CategoryType type,
        boolean predefined
) {
    public static CategoryResponse from(Category category) {
        return new CategoryResponse(
                category.getId(),
                category.getName(),
                category.getType(),
                category.isPredefined()
        );
    }
}