package info.eliumontoyasadec.cryptotracker.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun CryptoDetailScreen(symbol: String) {
    // Fake data mínima por símbolo
    val (qty, cost, pnl) = when (symbol.uppercase()) {
        "BTC" -> Triple(0.25, 9000.0, 500.0)
        "ETH" -> Triple(2.0, 2500.0, 300.0)
        "SOL" -> Triple(30.0, 1000.0, 40.0)
        else -> Triple(0.0, 0.0, 0.0)
    }

    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("Detalle: $symbol", style = MaterialTheme.typography.headlineSmall)

        ElevatedCard {
            Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("Cantidad: $qty")
                Text("USD adquirido: ${"%,.2f".format(cost)}")
                Text("P&L realizado (fake): ${"%,.2f".format(pnl)}")
            }
        }

        ElevatedCard {
            Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("Holdings por wallet (fake)", style = MaterialTheme.typography.titleMedium)
                Text("Metamask · ${(qty * 0.7)}")
                Text("ByBit · ${(qty * 0.3)}")
            }
        }

        ElevatedCard {
            Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("Movimientos (fake)", style = MaterialTheme.typography.titleMedium)
                Text("15 Feb 2025 · BUY · +0.10 \$X")
                Text("18 Feb 2025 · SELL · -0.05 \$Y")
            }
        }
    }
}