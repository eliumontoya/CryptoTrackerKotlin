package info.eliumontoyasadec.cryptotracker.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun PortfolioByCryptosScreen() {
    Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("Portafolio por Cryptos", style = MaterialTheme.typography.headlineSmall)
        ElevatedCard {
            Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("BTC  ·  Disponible: 0.25  ·  USD Adquirido: $9,000  ·  Ganancia: $500")
                Text("ETH  ·  Disponible: 2.00  ·  USD Adquirido: $2,500  ·  Ganancia: $300")
                Text("SOL  ·  Disponible: 30.0  ·  USD Adquirido: $1,000  ·  Ganancia: $40")
            }
        }
    }
}