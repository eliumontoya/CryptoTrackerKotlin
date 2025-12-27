package info.eliumontoyasadec.cryptotracker.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Divider
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

data class WalletBreakdownRow(
    val walletName: String,
    val valueUsd: Double,
    val pnlUsd: Double
)

private enum class SortBy(val label: String) {
    VALUE("Valor"),
    PNL("P&L")
}

@Composable
fun WalletBreakdownScreen(
    rows: List<WalletBreakdownRow>,
    onWalletClick: (walletName: String) -> Unit
) {
    // UI-only filters
    var showEmpty by remember { mutableStateOf(false) }
    var sortBy by remember { mutableStateOf(SortBy.VALUE) }

    val filtered = rows
        .asSequence()
        .filter { showEmpty || it.valueUsd > 0.0 || it.pnlUsd != 0.0 }
        .toList()

    val sorted = when (sortBy) {
        SortBy.VALUE -> filtered.sortedByDescending { it.valueUsd }
        SortBy.PNL -> filtered.sortedByDescending { it.pnlUsd }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("Desglose por Carteras", style = MaterialTheme.typography.headlineSmall)

        // Filters
        ElevatedCard {
            Column(
                modifier = Modifier.padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text("Filtros", style = MaterialTheme.typography.titleMedium)
                Divider()

                Text("Mostrar", style = MaterialTheme.typography.labelLarge)
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    item {
                        FilterChip(
                            selected = showEmpty,
                            onClick = { showEmpty = !showEmpty },
                            label = { Text("Incluir vacías") }
                        )
                    }
                }

                Spacer(Modifier.height(6.dp))

                Text("Orden", style = MaterialTheme.typography.labelLarge)
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(SortBy.entries) { opt ->
                        FilterChip(
                            selected = opt == sortBy,
                            onClick = { sortBy = opt },
                            label = { Text(opt.label) }
                        )
                    }
                }
            }
        }

        // List
        ElevatedCard {
            Column(
                modifier = Modifier.padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Carteras", style = MaterialTheme.typography.titleMedium)
                    Text("${sorted.size}", style = MaterialTheme.typography.labelLarge)
                }
                Divider()

                if (sorted.isEmpty()) {
                    Text(
                        text = "No hay carteras para mostrar con los filtros actuales.",
                        style = MaterialTheme.typography.bodyMedium
                    )
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(sorted, key = { it.walletName }) { row ->
                            WalletRowItem(row = row, onClick = { onWalletClick(row.walletName) })
                            Divider()
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun WalletRowItem(
    row: WalletBreakdownRow,
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
            Text(row.walletName, style = MaterialTheme.typography.titleMedium)
            Text(
                text = "Valor: ${formatUsd(row.valueUsd)}",
                style = MaterialTheme.typography.bodySmall
            )
        }
        Text(formatUsd(row.pnlUsd), style = MaterialTheme.typography.titleMedium)
    }
}

private fun formatUsd(v: Double): String = "$" + "%,.2f".format(v)