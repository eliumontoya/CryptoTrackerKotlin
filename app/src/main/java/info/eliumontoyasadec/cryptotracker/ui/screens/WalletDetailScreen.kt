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

@Composable
fun WalletDetailScreen(walletName: String) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = "Detalle: $walletName",
            style = MaterialTheme.typography.headlineSmall
        )

        // Resumen
        ElevatedCard {
            Column(
                modifier = Modifier.padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    text = "Valor total (fake): ${formatUsd(fakeValue(walletName))}",
                    style = MaterialTheme.typography.bodyMedium
                )
                Text(
                    text = "P&L total (fake): ${formatUsd(fakePnl(walletName))}",
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
                Text("BTC · 0.10")
                Text("ETH · 1.00")
                Text("SOL · 20.0")
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
                Text("15 Feb 2025 · BUY · +0.10 \\\$X")
                Text("18 Feb 2025 · SELL · -0.05 \\\$Y")
            }
        }
    }
}

private fun fakeValue(walletName: String): Double = when (walletName.trim().lowercase()) {
    "metamask" -> 10400.0
    "bybit" -> 2100.0
    "phantom" -> 0.0
    else -> 0.0
}

private fun fakePnl(walletName: String): Double = when (walletName.trim().lowercase()) {
    "metamask" -> 650.0
    "bybit" -> 190.0
    "phantom" -> 0.0
    else -> 0.0
}

private fun formatUsd(value: Double): String = "$" + "%,.2f".format(value)