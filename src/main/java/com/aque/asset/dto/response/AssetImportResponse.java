package com.aque.asset.dto.response;

import java.util.List;

public record AssetImportResponse(List<AssetResponse> created, List<AssetResponse> updated, List<AssetImportError> errors) {
}
