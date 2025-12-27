package info.eliumontoyasadec.cryptotracker.ui.portfolio

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun PortfolioHeader(
    title: String,
    lastUpdatedLabel: String?
) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(text = title, style = MaterialTheme.typography.headlineSmall)
        if (!lastUpdatedLabel.isNullOrBlank()) {
            Text(text = lastUpdatedLabel, style = MaterialTheme.typography.bodySmall)
        }
    }
}

@Composable
fun PortfolioSummary(
    investedUsd: Double,
    realizedPnlUsd: Double
) {
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        StatCard(
            title = "Invertido (USD)",
            value = formatUsd(investedUsd),
            modifier = Modifier.weight(1f)
        )
        StatCard(
            title = "Realizado (USD)",
            value = formatUsd(realizedPnlUsd),
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
fun StatCard(
    title: String,
    value: String,
    modifier: Modifier = Modifier
) {
    ElevatedCard(modifier = modifier) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(text = title, style = MaterialTheme.typography.labelMedium)
            Text(text = value, style = MaterialTheme.typography.titleLarge)
        }
    }
}

@Composable
fun HoldingsCard(
    rows: List<PortfolioUiState.Row>,
    onRowClick: (String) -> Unit
) {
    ElevatedCard {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("Holdings", style = MaterialTheme.typography.titleMedium)
            Divider()

            LazyColumn(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                items(rows, key = { it.symbol }) { row ->
                    HoldingRow(row = row, onClick = { onRowClick(row.symbol) })
                    Divider()
                }
            }
        }
    }
}

@Composable
fun HoldingRow(
    row: PortfolioUiState.Row,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 10.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(Modifier.weight(1f)) {
            Text(row.symbol, style = MaterialTheme.typography.titleMedium)
            Text(
                "Qty: ${formatQty(row.quantity)}  ·  Cost: ${formatUsd(row.costUsd)}",
                style = MaterialTheme.typography.bodySmall
            )
        }
        Text(
            formatUsd(row.realizedPnlUsd),
            style = MaterialTheme.typography.titleMedium
        )
    }
}
@Composable
fun PortfolioEmptyState() {
    ElevatedCard {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text("Aún no tienes holdings.", style = MaterialTheme.typography.titleMedium)
            Text("Registra tu primera compra para comenzar.", style = MaterialTheme.typography.bodyMedium)
        }
    }
}