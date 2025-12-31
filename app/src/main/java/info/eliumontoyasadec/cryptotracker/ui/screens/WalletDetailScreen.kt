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
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

// -------- UI models --------

data class WalletCryptoRow(
    val symbol: String,
    val quantity: Double,
    val valueUsd: Double,
    val pnlUsd: Double
)

data class WalletMovementRow(
    val dateLabel: String,
    val description: String
)

// -------- UI State --------

data class WalletDetailUiState(
    val walletName: String = "",
    val totalValueUsd: Double = 0.0,
    val totalPnlUsd: Double = 0.0,
    val cryptos: List<WalletCryptoRow> = emptyList(),
    val movements: List<WalletMovementRow> = emptyList()
)

private enum class WalletDetailTab(val label: String) {
    HOLDINGS("Holdings"),
    MOVEMENTS("Movimientos")
}

// -------- Composable --------

@Composable
fun WalletDetailScreen(state: WalletDetailUiState) {
    var selectedTabIndex by remember { mutableIntStateOf(0) }
    val tabs = WalletDetailTab.entries

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = "Detalle: ${state.walletName}",
            style = MaterialTheme.typography.headlineSmall
        )

        WalletSummaryCard(
            totalValueUsd = state.totalValueUsd,
            totalPnlUsd = state.totalPnlUsd
        )

        TabRow(selectedTabIndex = selectedTabIndex) {
            tabs.forEachIndexed { idx, tab ->
                Tab(
                    selected = idx == selectedTabIndex,
                    onClick = { selectedTabIndex = idx },
                    text = { Text(tab.label) }
                )
            }
        }

        when (tabs[selectedTabIndex]) {
            WalletDetailTab.HOLDINGS -> WalletHoldingsList(state.cryptos)
            WalletDetailTab.MOVEMENTS -> WalletMovementsList(state.movements)
        }
    }
}

@Composable
private fun WalletSummaryCard(totalValueUsd: Double, totalPnlUsd: Double) {
    ElevatedCard {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text("Resumen", style = MaterialTheme.typography.titleMedium)
            HorizontalDivider()

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Valor total", style = MaterialTheme.typography.bodyMedium)
                Text(formatUsd(totalValueUsd), style = MaterialTheme.typography.titleMedium)
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("P&L", style = MaterialTheme.typography.bodyMedium)
                Text(formatUsd(totalPnlUsd), style = MaterialTheme.typography.titleMedium)
            }

            Text(
                text = "(datos fake)",
                style = MaterialTheme.typography.labelSmall
            )
        }
    }
}

@Composable
private fun WalletHoldingsList(rows: List<WalletCryptoRow>) {
    ElevatedCard {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Holdings", style = MaterialTheme.typography.titleMedium)
                Text("${rows.size}", style = MaterialTheme.typography.labelLarge)
            }
            HorizontalDivider()

            if (rows.isEmpty()) {
                Text(
                    text = "No hay cryptos en esta cartera.",
                    style = MaterialTheme.typography.bodyMedium
                )
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(rows, key = { it.symbol }) { r ->
                        HoldingRowItem(r)
                        HorizontalDivider()
                    }
                }
            }
        }
    }
}

@Composable
private fun HoldingRowItem(row: WalletCryptoRow) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(row.symbol, style = MaterialTheme.typography.titleMedium)
            Text(formatUsd(row.valueUsd), style = MaterialTheme.typography.titleMedium)
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text("Cantidad: ${formatQty(row.quantity)}", style = MaterialTheme.typography.bodySmall)
            Text("P&L: ${formatUsd(row.pnlUsd)}", style = MaterialTheme.typography.bodySmall)
        }
    }
}

@Composable
private fun WalletMovementsList(rows: List<WalletMovementRow>) {
    ElevatedCard {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Movimientos", style = MaterialTheme.typography.titleMedium)
                Text("${rows.size}", style = MaterialTheme.typography.labelLarge)
            }
            HorizontalDivider()

            if (rows.isEmpty()) {
                Text(
                    text = "No hay movimientos en esta cartera.",
                    style = MaterialTheme.typography.bodyMedium
                )
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(rows, key = { it.dateLabel + it.description }) { r ->
                        MovementRowItem(r)
                        HorizontalDivider()
                    }
                }
            }

            Spacer(Modifier.height(2.dp))
            Text(
                text = "(datos fake)",
                style = MaterialTheme.typography.labelSmall
            )
        }
    }
}

@Composable
private fun MovementRowItem(row: WalletMovementRow) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(row.dateLabel, style = MaterialTheme.typography.labelMedium)
        Text(row.description, style = MaterialTheme.typography.bodyMedium)
    }
}

// -------- ViewModel (fake data) --------

class WalletDetailViewModel(private val walletName: String) : ViewModel() {

    private val _state = MutableStateFlow(buildFakeState(walletName))
    val state: StateFlow<WalletDetailUiState> = _state.asStateFlow()

    companion object {
        fun buildFakeState(walletName: String): WalletDetailUiState {
            val name = walletName.trim()
            val cryptos = fakeHoldingsFor(name)
            return WalletDetailUiState(
                walletName = name,
                totalValueUsd = cryptos.sumOf { it.valueUsd },
                totalPnlUsd = cryptos.sumOf { it.pnlUsd },
                cryptos = cryptos,
                movements = fakeMovementsFor(name)
            )
        }
    }

    class Factory(private val walletName: String) : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(WalletDetailViewModel::class.java)) {
                @Suppress("UNCHECKED_CAST")
                return WalletDetailViewModel(walletName) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class")
        }
    }
}

// -------- Fake helpers --------

private fun fakeHoldingsFor(walletName: String): List<WalletCryptoRow> {
    return when (walletName.lowercase()) {
        "metamask" -> listOf(
            WalletCryptoRow(symbol = "BTC", quantity = 0.10, valueUsd = 4_000.0, pnlUsd = 250.0),
            WalletCryptoRow(symbol = "ETH", quantity = 1.00, valueUsd = 2_400.0, pnlUsd = 120.0),
            WalletCryptoRow(symbol = "SOL", quantity = 20.0, valueUsd = 4_000.0, pnlUsd = 280.0)
        )
        "bybit" -> listOf(
            WalletCryptoRow(symbol = "AIXBT", quantity = 12.0, valueUsd = 1_500.0, pnlUsd = 150.0),
            WalletCryptoRow(symbol = "ALGO", quantity = 800.0, valueUsd = 600.0, pnlUsd = 40.0)
        )
        "phantom" -> emptyList()
        else -> emptyList()
    }
}

private fun fakeMovementsFor(walletName: String): List<WalletMovementRow> {
    return when (walletName.lowercase()) {
        "metamask" -> listOf(
            WalletMovementRow("15 Feb 2025", "BUY · +0.10 BTC"),
            WalletMovementRow("18 Feb 2025", "BUY · +1.00 ETH"),
            WalletMovementRow("20 Feb 2025", "SWAP · -5 ALGO +1 AIXBT")
        )
        "bybit" -> listOf(
            WalletMovementRow("12 Feb 2025", "DEPOSIT · +12 AIXBT"),
            WalletMovementRow("13 Feb 2025", "FEE · -0.10 AIXBT")
        )
        "phantom" -> emptyList()
        else -> emptyList()
    }
}

private fun formatUsd(value: Double): String = "$" + "%,.2f".format(value)

private fun formatQty(value: Double): String {
    // clean, readable quantity formatting for demo UI
    return when {
        value >= 1000.0 -> "%,.0f".format(value)
        value >= 10.0 -> "%,.2f".format(value)
        value >= 1.0 -> "%,.4f".format(value)
        else -> "%,.6f".format(value)
    }
}