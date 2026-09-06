package com.aque.asset.dto.response;

public record AssetImportError(String sheet, int row, String message, boolean informational) {
}
