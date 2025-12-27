package info.eliumontoyasadec.cryptotracker.ui.portfolio

import info.eliumontoyasadec.cryptotracker.ui.portfolio.PortfolioUiState

object PortfolioFakeData {
    val sample = PortfolioUiState(
        title = "Portafolio Principal",
        investedUsd = 12_500.0,
        realizedPnlUsd = 840.0,
        lastUpdatedLabel = "Actualizado: hace 2 min",
        rows = listOf(
            PortfolioUiState.Row("BTC", 0.25, 9_000.0, 500.0),
            PortfolioUiState.Row("ETH", 2.00, 2_500.0, 300.0),
            PortfolioUiState.Row("SOL", 30.0, 1_000.0, 40.0),
        )
    )

    val empty = PortfolioUiState(
        title = "Portafolio Vacío",
        investedUsd = 0.0,
        realizedPnlUsd = 0.0,
        lastUpdatedLabel = null,
        rows = emptyList()
    )
}