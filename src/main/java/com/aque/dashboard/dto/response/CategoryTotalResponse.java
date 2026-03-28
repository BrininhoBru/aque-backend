package com.aque.dashboard.dto.response;

import com.aque.category.dto.response.CategoryResponse;

import java.math.BigDecimal;

public record CategoryTotalResponse(
        CategoryResponse category,
        BigDecimal totalExpected,
        BigDecimal totalPaid
) {}