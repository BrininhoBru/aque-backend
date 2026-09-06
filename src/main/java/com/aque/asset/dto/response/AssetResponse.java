package com.aque.asset.dto.response;

import com.aque.asset.Asset;
import com.aque.asset.AssetType;
import com.aque.person.dto.response.PersonResponse;

import java.math.BigDecimal;
import java.util.UUID;

public record AssetResponse(
        UUID id,
        String name,
        AssetType type,
        BigDecimal currentValue,
        PersonResponse person
) {
    public static AssetResponse from(Asset asset) {
        return new AssetResponse(
                asset.getId(),
                asset.getName(),
                asset.getType(),
                asset.getCurrentValue(),
                asset.getPerson() != null ? PersonResponse.from(asset.getPerson()) : null
        );
    }
}
