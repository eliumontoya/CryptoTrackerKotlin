package info.eliumontoyasadec.cryptotracker.ui.screens

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

enum class MovementMode { IN, OUT, BETWEEN, SWAP }

private enum class WalletFilter(val label: String) {
    ALL("Todas"),
    METAMASK("Metamask"),
    BYBIT("ByBit"),
    PHANTOM("Phantom")
}

private enum class CryptoFilter(val label: String) {
    ALL("Todas"),
    BTC("BTC"),
    ETH("ETH"),
    SOL("SOL"),
    ALGO("ALGO"),
    AIXBT("AIXBT")
}

private data class MovementRow(
    val id: String,
    val dateLabel: String,
    val wallet: WalletFilter,
    val crypto: CryptoFilter,
    val headline: String,
    val details: String
)

@Composable
fun MovementsScreen(title: String, mode: MovementMode) {
    // UI-only filter state
    var walletFilter by remember { mutableStateOf(WalletFilter.ALL) }
    var cryptoFilter by remember { mutableStateOf(CryptoFilter.ALL) }

    val allRows = remember(mode) { fakeRowsFor(mode) }
    val filteredRows = allRows.filter { row ->
        (walletFilter == WalletFilter.ALL || row.wallet == walletFilter) &&
            (cryptoFilter == CryptoFilter.ALL || row.crypto == cryptoFilter)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(title, style = MaterialTheme.typography.headlineSmall)

        // Filters
        ElevatedCard {
            Column(
                modifier = Modifier.padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text("Filtros", style = MaterialTheme.typography.titleMedium)
                Divider()

                Text("Cartera", style = MaterialTheme.typography.labelLarge)
                ChipRow(
                    options = WalletFilter.entries,
                    selected = walletFilter,
                    labelOf = { it.label },
                    onSelect = { walletFilter = it }
                )

                Spacer(Modifier.height(6.dp))

                Text("Crypto", style = MaterialTheme.typography.labelLarge)
                ChipRow(
                    options = CryptoFilter.entries,
                    selected = cryptoFilter,
                    labelOf = { it.label },
                    onSelect = { cryptoFilter = it }
                )
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
                    Text("Movimientos", style = MaterialTheme.typography.titleMedium)
                    Text("${filteredRows.size}", style = MaterialTheme.typography.labelLarge)
                }
                Divider()

                if (filteredRows.isEmpty()) {
                    Text(
                        text = "No hay movimientos con los filtros actuales.",
                        style = MaterialTheme.typography.bodyMedium
                    )
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(filteredRows, key = { it.id }) { row ->
                            MovementRowItem(row)
                            Divider()
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun <T> ChipRow(
    options: List<T>,
    selected: T,
    labelOf: (T) -> String,
    onSelect: (T) -> Unit
) {
    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        items(options) { opt ->
            FilterChip(
                selected = opt == selected,
                onClick = { onSelect(opt) },
                label = { Text(labelOf(opt)) }
            )
        }
    }
}

@Composable
private fun MovementRowItem(row: MovementRow) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(row.headline, style = MaterialTheme.typography.titleSmall)
            Text(row.dateLabel, style = MaterialTheme.typography.labelMedium)
        }
        Text(
            text = "${row.wallet.label} · ${row.crypto.label}",
            style = MaterialTheme.typography.bodySmall
        )
        Text(row.details, style = MaterialTheme.typography.bodyMedium)
    }
}

private fun fakeRowsFor(mode: MovementMode): List<MovementRow> {
    return when (mode) {
        MovementMode.IN -> listOf(
            MovementRow(
                id = "in-1",
                dateLabel = "15 Feb 2025",
                wallet = WalletFilter.METAMASK,
                crypto = CryptoFilter.BTC,
                headline = "+ 0.10 BTC",
                details = "Entrada a Metamask (fake)"
            ),
            MovementRow(
                id = "in-2",
                dateLabel = "12 Feb 2025",
                wallet = WalletFilter.BYBIT,
                crypto = CryptoFilter.AIXBT,
                headline = "+ 12 AIXBT",
                details = "Entrada a ByBit (fake)"
            )
        )

        MovementMode.OUT -> listOf(
            MovementRow(
                id = "out-1",
                dateLabel = "16 Feb 2025",
                wallet = WalletFilter.METAMASK,
                crypto = CryptoFilter.BTC,
                headline = "- 0.05 BTC",
                details = "Salida desde Metamask (fake)"
            ),
            MovementRow(
                id = "out-2",
                dateLabel = "13 Feb 2025",
                wallet = WalletFilter.PHANTOM,
                crypto = CryptoFilter.SOL,
                headline = "- 1 SOL",
                details = "Salida desde Phantom (fake)"
            )
        )

        MovementMode.BETWEEN -> listOf(
            MovementRow(
                id = "btw-1",
                dateLabel = "14 Feb 2025",
                wallet = WalletFilter.METAMASK,
                crypto = CryptoFilter.ALGO,
                headline = "Metamask → Phantom",
                details = "3 ALGO transferidos (fake)"
            ),
            MovementRow(
                id = "btw-2",
                dateLabel = "12 Feb 2025",
                wallet = WalletFilter.BYBIT,
                crypto = CryptoFilter.AIXBT,
                headline = "ByBit → Metamask",
                details = "2 AIXBT transferidos (fake)"
            )
        )

        MovementMode.SWAP -> listOf(
            MovementRow(
                id = "sw-1",
                dateLabel = "13 Feb 2025",
                wallet = WalletFilter.METAMASK,
                crypto = CryptoFilter.AIXBT,
                headline = "Swap en Metamask",
                details = "+ 1 AIXBT · - 5 ALGO (fake)"
            ),
            MovementRow(
                id = "sw-2",
                dateLabel = "11 Feb 2025",
                wallet = WalletFilter.PHANTOM,
                crypto = CryptoFilter.SOL,
                headline = "Swap en Phantom",
                details = "+ 2 SOL · - 0.01 BTC (fake)"
            )
        )
    }
}