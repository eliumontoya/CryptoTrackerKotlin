package info.eliumontoyasadec.cryptotracker.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
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

data class WalletCryptoRow(
    val symbol: String,
    val quantity: Double
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

// -------- Composable --------

@Composable
fun WalletDetailScreen(state: WalletDetailUiState) {
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

        // Resumen
        ElevatedCard {
            Column(
                modifier = Modifier.padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    text = "Valor total (fake): ${formatUsd(state.totalValueUsd)}",
                    style = MaterialTheme.typography.bodyMedium
                )
                Text(
                    text = "P&L total (fake): ${formatUsd(state.totalPnlUsd)}",
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }

        // Cryptos
        ElevatedCard {
            Column(
                modifier = Modifier.padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    text = "Cryptos en la cartera (fake)",
                    style = MaterialTheme.typography.titleMedium
                )
                state.cryptos.forEach {
                    Text("${it.symbol} · ${it.quantity}")
                }
            }
        }

        // Movimientos
        ElevatedCard {
            Column(
                modifier = Modifier.padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    text = "Movimientos (fake)",
                    style = MaterialTheme.typography.titleMedium
                )
                state.movements.forEach {
                    Text("${it.dateLabel} · ${it.description}")
                }
            }
        }
    }
}

// -------- ViewModel (fake data) --------

class WalletDetailViewModel(private val walletName: String) : ViewModel() {

    private val _state = MutableStateFlow(buildFakeState(walletName))
    val state: StateFlow<WalletDetailUiState> = _state.asStateFlow()

    companion object {
        fun buildFakeState(walletName: String): WalletDetailUiState {
            val name = walletName.trim()
            return WalletDetailUiState(
                walletName = name,
                totalValueUsd = fakeValue(name),
                totalPnlUsd = fakePnl(name),
                cryptos = listOf(
                    WalletCryptoRow("BTC", 0.10),
                    WalletCryptoRow("ETH", 1.00),
                    WalletCryptoRow("SOL", 20.0)
                ),
                movements = listOf(
                    WalletMovementRow("15 Feb 2025", "BUY · +0.10 BTC"),
                    WalletMovementRow("18 Feb 2025", "SELL · -0.05 BTC")
                )
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

private fun fakeValue(walletName: String): Double = when (walletName.lowercase()) {
    "metamask" -> 10400.0
    "bybit" -> 2100.0
    "phantom" -> 0.0
    else -> 0.0
}

private fun fakePnl(walletName: String): Double = when (walletName.lowercase()) {
    "metamask" -> 650.0
    "bybit" -> 190.0
    "phantom" -> 0.0
    else -> 0.0
}

private fun formatUsd(value: Double): String = "$" + "%,.2f".format(value)