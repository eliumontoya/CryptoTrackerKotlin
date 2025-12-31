package info.eliumontoyasadec.cryptotracker.ui.admin

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Download
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import info.eliumontoyasadec.cryptotracker.data.seed.SeedRequest
import info.eliumontoyasadec.cryptotracker.ui.shell.LocalAppDeps
import kotlinx.coroutines.launch

@Composable
fun LoadInitialCatalogsScreen(
    onClose: () -> Unit
) {
    val deps = LocalAppDeps.current
    val scope = rememberCoroutineScope()

    var wallets by remember { mutableStateOf(true) }
    var cryptos by remember { mutableStateOf(true) }
    var fiat by remember { mutableStateOf(true) }
    var syncManual by remember { mutableStateOf(false) }

    var loading by remember { mutableStateOf(false) }
    var lastResult by remember { mutableStateOf<String?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Header similar al mock
        Icon(
            imageVector = Icons.Default.Download,
            contentDescription = null,
            modifier = Modifier.size(40.dp)
        )

        Text("Carga de Catálogos Iniciales", style = MaterialTheme.typography.headlineSmall)
        Text(
            "Activa los elementos que deseas crear en la base de datos con datos predeterminados.",
            style = MaterialTheme.typography.bodySmall
        )

        ElevatedCard {
            Column(
                modifier = Modifier.padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                CatalogToggleRow(
                    title = "Carteras",
                    subtitle = "Crea portafolio default y wallets base",
                    checked = wallets,
                    onCheckedChange = { wallets = it }
                )
                Divider()
                CatalogToggleRow(
                    title = "Cryptos",
                    subtitle = "Crea catálogo de cryptos (BTC, ETH, SOL, USDT...)",
                    checked = cryptos,
                    onCheckedChange = { cryptos = it }
                )
                Divider()
                CatalogToggleRow(
                    title = "FIAT",
                    subtitle = "Crea catálogo fiat (USD, MXN, EUR...)",
                    checked = fiat,
                    onCheckedChange = { fiat = it }
                )
                Divider()
                CatalogToggleRow(
                    title = "Sync Manual",
                    subtitle = "Aplica configuración base (placeholder listo para crecer)",
                    checked = syncManual,
                    onCheckedChange = { syncManual = it }
                )
            }
        }

        ElevatedCard {
            Column(
                modifier = Modifier.padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Button(
                    onClick = {
                        loading = true
                        lastResult = null
                        scope.launch {
                            try {
                                val res = deps.catalogSeeder.seed(
                                    SeedRequest(
                                        wallets = wallets,
                                        cryptos = cryptos,
                                        fiat = fiat,
                                        syncManual = syncManual
                                    )
                                )
                                lastResult =
                                    "OK: wallets=${res.walletsInserted}, cryptos=${res.cryptosUpserted}, fiat=${res.fiatUpserted}"
                            } catch (t: Throwable) {
                                lastResult = "Error: ${t.message ?: "fallo desconocido"}"
                            } finally {
                                loading = false
                            }
                        }
                    },
                    enabled = !loading && (wallets || cryptos || fiat || syncManual),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    if (loading) {
                        CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                        Spacer(Modifier.width(10.dp))
                    }
                    Text("Iniciar Carga")
                }

                if (lastResult != null) {
                    Text(lastResult!!, style = MaterialTheme.typography.bodySmall)
                }
            }
        }

        Spacer(Modifier.weight(1f))

        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
            OutlinedButton(onClick = onClose) {
                Text("Cerrar")
            }
        }
    }
}

@Composable
private fun CatalogToggleRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleSmall)
            Text(subtitle, style = MaterialTheme.typography.bodySmall)
        }
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}