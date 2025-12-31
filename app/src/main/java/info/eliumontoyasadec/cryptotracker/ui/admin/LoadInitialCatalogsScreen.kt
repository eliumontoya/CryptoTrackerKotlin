package info.eliumontoyasadec.cryptotracker.ui.admin

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Download
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import info.eliumontoyasadec.cryptotracker.data.seed.CatalogStatus
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

    var showConfirm by remember { mutableStateOf(false) }
    var pendingRequest by remember { mutableStateOf<SeedRequest?>(null) }

    var status by remember { mutableStateOf<CatalogStatus?>(null) }

    LaunchedEffect(Unit) {
        status = deps.catalogSeeder.status()
    }

    val s = status
    val walletsAlready = (s?.wallets ?: 0) > 0
    val cryptosAlready = (s?.cryptos ?: 0) > 0
    val fiatAlready = (s?.fiat ?: 0) > 0


    fun buildEffectiveRequest(): SeedRequest {

        return SeedRequest(
            wallets = wallets && !walletsAlready,
            cryptos = cryptos && !cryptosAlready,
            fiat = fiat && !fiatAlready,
            syncManual = syncManual // este no depende de catálogos aún
        )
    }

    fun requestSeed() {
        pendingRequest = buildEffectiveRequest()

        showConfirm = true
    }

    fun confirmSeed() {
        val req = pendingRequest ?: return
        showConfirm = false
        loading = true
        lastResult = null

        scope.launch {
            val resultText = try {
                val res = deps.catalogSeeder.seed(req)
                buildString {
                    append("OK: ")
                    append("wallets=${res.walletsInserted}, ")
                    append("cryptos=${res.cryptosUpserted}, ")
                    append("fiat=${res.fiatUpserted}")
                }
            } catch (t: Throwable) {
                "Error: ${t.message ?: "fallo desconocido"}"
            } finally {
                loading = false
                pendingRequest = null
            }

            lastResult = resultText
        }
    }


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
                    subtitle = if (walletsAlready) "Ya existe data (bloqueado)" else "Crea portafolio default y wallets base",
                    checked = wallets,
                    enabled = !walletsAlready && !loading,

                    onCheckedChange = { wallets = it }
                )

                HorizontalDivider()
                CatalogToggleRow(
                    title = "Cryptos",
                    subtitle = if (cryptosAlready) "Ya existe data (bloqueado)" else "Crea catálogo de cryptos (BTC, ETH, SOL, USDT...)",
                    checked = cryptos,
                    enabled = !cryptosAlready && !loading,

                    onCheckedChange = { cryptos = it }
                )
                HorizontalDivider()
                CatalogToggleRow(
                    title = "FIAT",
                    subtitle = if (fiatAlready) "Ya existe data (bloqueado)" else "Crea catálogo fiat (USD, MXN, EUR...)",
                    checked = fiat,
                    enabled = !fiatAlready && !loading,

                    onCheckedChange = { fiat = it }
                )
                HorizontalDivider()
                CatalogToggleRow(
                    title = "Sync Manual",
                    subtitle = "Aplica configuración base (placeholder listo para crecer)",
                    checked = syncManual,
                    enabled = false,
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
                    onClick = { requestSeed() },

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

    if (showConfirm) {
        val req = pendingRequest
        AlertDialog(
            onDismissRequest = { showConfirm = false },
            title = { Text("Confirmar carga") },
            text = {
                Text(
                    buildString {
                        appendLine("Se crearán datos predeterminados en la base de datos:")
                        if (req?.wallets == true) appendLine("• Portafolio + Carteras")
                        if (req?.cryptos == true) appendLine("• Catálogo de Cryptos")
                        if (req?.fiat == true) appendLine("• Catálogo FIAT")
                        if (req?.syncManual == true) appendLine("• Configuración Sync Manual")
                    }
                )
            },
            confirmButton = {
                Button(onClick = { confirmSeed() }) { Text("Confirmar") }
            },
            dismissButton = {
                OutlinedButton(onClick = { showConfirm = false }) { Text("Cancelar") }
            }
        )
    }
}

@Composable
private fun CatalogToggleRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    enabled: Boolean,
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
        Switch(
            checked = checked,
            onCheckedChange = { if (enabled) onCheckedChange(it) },
            enabled = enabled
        )
    }
}