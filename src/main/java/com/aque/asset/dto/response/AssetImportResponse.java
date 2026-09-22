package com.aque.asset.dto.response;

import java.math.BigDecimal;
import java.util.List;

/**
 * @param missing ativos que vieram de um import anterior (têm `externalCode`) mas não estão
 *                no arquivo — vendidos, vencidos, ou arquivo parcial. O import nunca apaga:
 *                só reporta, e quem decide é o usuário.
 */
public record AssetImportResponse(
        List<AssetResponse> created,
        List<AssetResponse> updated,
        List<AssetResponse> missing,
        List<AssetImportError> errors,
        List<AssetImportSheetSummary> sheets,
        BigDecimal totalRead,
        BigDecimal totalPersisted
) {
}
