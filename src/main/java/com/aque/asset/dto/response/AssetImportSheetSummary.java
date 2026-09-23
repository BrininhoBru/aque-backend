package com.aque.asset.dto.response;

import java.math.BigDecimal;

/**
 * Reconciliação de uma aba do extrato: o que foi lido do arquivo vs. o que virou ativo.
 * Os dois totais divergirem significa que linhas distintas colapsaram no mesmo ativo — é o
 * sinal que faltava quando o bug da #37 passou meses despercebido.
 */
public record AssetImportSheetSummary(
        String sheet,
        int rows,
        BigDecimal totalRead,
        BigDecimal totalPersisted
) {
}
