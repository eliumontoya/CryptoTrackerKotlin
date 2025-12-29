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
import androidx.compose.material3.Divider
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

// -------- UI models --------

data class CryptoWalletRow(
    val walletName: String,
    val quantity: Double
)

data class CryptoMovementRow(
    val dateLabel: String,
    val description: String
)

// -------- UI State --------

data class CryptoDetailUiState(
    val symbol: String = "",
    val totalQty: Double = 0.0,
    val totalCostUsd: Double = 0.0,
    val totalPnlUsd: Double = 0.0,
    val byWallet: List<CryptoWalletRow> = emptyList(),
    val movements: List<CryptoMovementRow> = emptyList()
)

// -------- Composable --------

@Composable
fun CryptoDetailScreen(state: CryptoDetailUiState) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("Detalle: ${state.symbol}", style = MaterialTheme.typography.headlineSmall)

        // Summary
        ElevatedCard {
            Column(
                modifier = Modifier.padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text("Resumen", style = MaterialTheme.typography.titleMedium)
                Divider()
                SummaryRow("Cantidad total", formatQty(state.totalQty))
                SummaryRow("USD adquirido", formatUsd(state.totalCostUsd))
                SummaryRow("P&L (fake)", formatUsd(state.totalPnlUsd))
            }
        }

        // By wallet
        ElevatedCard {
            Column(
                modifier = Modifier.padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Distribución por cartera", style = MaterialTheme.typography.titleMedium)
                    Text("${state.byWallet.size}", style = MaterialTheme.typography.labelLarge)
                }
                Divider()
                if (state.byWallet.isEmpty()) {
                    Text("No hay holdings para esta crypto.")
                } else {
                    state.byWallet.forEach {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(it.walletName)
                            Text(formatQty(it.quantity))
                        }
                    }
                }
            }
        }

        // Movements
        ElevatedCard {
            Column(
                modifier = Modifier.padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Movimientos", style = MaterialTheme.typography.titleMedium)
                    Text("${state.movements.size}", style = MaterialTheme.typography.labelLarge)
                }
                Divider()
                if (state.movements.isEmpty()) {
                    Text("No hay movimientos para esta crypto.")
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(state.movements) { m ->
                            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                Text(m.dateLabel, style = MaterialTheme.typography.labelMedium)
                                Text(m.description)
                            }
                            Divider()
                        }
                    }
                }
            }
        }

        Spacer(Modifier.height(2.dp))
        Text("(datos fake)", style = MaterialTheme.typography.labelSmall)
    }
}

@Composable
private fun SummaryRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label)
        Text(value, style = MaterialTheme.typography.titleMedium)
    }
}

// -------- ViewModel (fake data) --------

class CryptoDetailViewModel(private val symbol: String) : ViewModel() {

    private val _state = MutableStateFlow(buildFakeState(symbol))
    val state: StateFlow<CryptoDetailUiState> = _state.asStateFlow()

    companion object {
        fun buildFakeState(symbol: String): CryptoDetailUiState {
            val sym = symbol.uppercase()
            val wallets = fakeByWallet(sym)
            val totalQty = wallets.sumOf { it.quantity }
            return CryptoDetailUiState(
                symbol = sym,
                totalQty = totalQty,
                totalCostUsd = fakeCost(sym),
                totalPnlUsd = fakePnl(sym),
                byWallet = wallets,
                movements = fakeMovements(sym)
            )
        }
    }

    class Factory(private val symbol: String) : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(CryptoDetailViewModel::class.java)) {
                @Suppress("UNCHECKED_CAST")
                return CryptoDetailViewModel(symbol) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class")
        }
    }
}

// -------- Fake helpers --------

private fun fakeByWallet(symbol: String): List<CryptoWalletRow> = when (symbol) {
    "BTC" -> listOf(
        CryptoWalletRow("Metamask", 0.175),
        CryptoWalletRow("ByBit", 0.075)
    )
    "ETH" -> listOf(
        CryptoWalletRow("Metamask", 1.4),
        CryptoWalletRow("ByBit", 0.6)
    )
    "SOL" -> listOf(
        CryptoWalletRow("Phantom", 20.0),
        CryptoWalletRow("Metamask", 10.0)
    )
    else -> emptyList()
}

private fun fakeMovements(symbol: String): List<CryptoMovementRow> = when (symbol) {
    "BTC" -> listOf(
        CryptoMovementRow("15 Feb 2025", "BUY · +0.10 BTC"),
        CryptoMovementRow("18 Feb 2025", "SELL · -0.05 BTC")
    )
    "ETH" -> listOf(
        CryptoMovementRow("10 Feb 2025", "BUY · +1.00 ETH"),
        CryptoMovementRow("20 Feb 2025", "BUY · +1.00 ETH")
    )
    "SOL" -> listOf(
        CryptoMovementRow("12 Feb 2025", "DEPOSIT · +20 SOL"),
        CryptoMovementRow("14 Feb 2025", "SWAP · +10 SOL")
    )
    else -> emptyList()
}

private fun fakeCost(symbol: String): Double = when (symbol) {
    "BTC" -> 9000.0
    "ETH" -> 2500.0
    "SOL" -> 1000.0
    else -> 0.0
}

private fun fakePnl(symbol: String): Double = when (symbol) {
    "BTC" -> 500.0
    "ETH" -> 300.0
    "SOL" -> 40.0
    else -> 0.0
}

private fun formatUsd(value: Double): String = "$" + "%,.2f".format(value)

private fun formatQty(value: Double): String = when {
    value >= 1000.0 -> "%,.0f".format(value)
    value >= 10.0 -> "%,.2f".format(value)
    value >= 1.0 -> "%,.4f".format(value)
    else -> "%,.6f".format(value)
}