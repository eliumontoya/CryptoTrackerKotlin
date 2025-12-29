package info.eliumontoyasadec.cryptotracker.domain.views

/**
 * Read models for UI. These are built by Queries (read-only) and are not persisted.
 *
 * Notes:
 * - Prices are optional for now (no prices table yet). When currentValueUsd is null,
 *   totalPnl* fields should be treated as unavailable (null).
 */
data class PortfolioSummaryView(
    val portfolioId: Long,

    // From holdings / ledger (Camino 2). For now, Camino 1 will feed 0.0.
    val investedUsd: Double,
    val realizedSalesUsd: Double,
    val realizedPnlUsd: Double,

    // Requires prices (optional for now)
    val currentValueUsd: Double? = null,

    // Convenience totals (optional when prices are missing)
    val totalPnlUsd: Double? = null,
    val totalPnlPct: Double? = null,

    // Useful for UI refresh hints
    val updatedAt: Long? = null
)

/**
 * Row for the "Portfolio by Cryptos" table.
 */
data class PortfolioByCryptoRowView(
    val portfolioId: Long,
    val cryptoSymbol: String,

    // Net position
    val quantity: Double,

    // Cost & realized metrics (Camino 2). Camino 1 can keep these as 0.0.
    val costUsd: Double,
    val realizedSalesUsd: Double,
    val realizedPnlUsd: Double,

    // Optional market price
    val currentPriceUsd: Double? = null
) {
    val avgCostUsd: Double? = safeDiv(costUsd, quantity)

    val currentValueUsd: Double? = currentPriceUsd?.let { it * quantity }

    val unrealizedPnlUsd: Double? = currentValueUsd?.let { it - costUsd }

    val totalPnlUsd: Double? =
        if (currentValueUsd == null) null else (realizedPnlUsd + (unrealizedPnlUsd ?: 0.0))

    /**
     * Total PnL percentage relative to invested cost basis.
     * If costUsd is 0, pct is undefined.
     */
    val totalPnlPct: Double? = totalPnlUsd?.let { pnl -> safeDiv(pnl, costUsd)?.times(100.0) }
}

private fun safeDiv(num: Double, den: Double): Double? {
    if (den == 0.0) return null
    return num / den
}