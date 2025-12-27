package info.eliumontoyasadec.cryptotracker.ui.portfolio

data class PortfolioUiState(
    val title: String,
    val investedUsd: Double,
    val realizedPnlUsd: Double,
    val rows: List<Row>,
    val lastUpdatedLabel: String? = null,
) {
    data class Row(
        val symbol: String,
        val quantity: Double,
        val costUsd: Double,
        val realizedPnlUsd: Double,
    )
}