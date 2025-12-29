package info.eliumontoyasadec.cryptotracker.ui.portfolio

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp


@Composable
fun PortfolioScreen(
    state: PortfolioUiState,
    onRowClick: (symbol: String) -> Unit = {}
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        PortfolioHeader(
            title = state.title,
            lastUpdatedLabel = state.lastUpdatedLabel
        )

        PortfolioSummary(
            investedUsd = state.investedUsd,
            realizedPnlUsd = state.realizedPnlUsd
        )

        if (state.rows.isEmpty()) {
            PortfolioEmptyState()
        } else {
            HoldingsCard(
                rows = state.rows,
                onRowClick = onRowClick
            )
        }
    }
}