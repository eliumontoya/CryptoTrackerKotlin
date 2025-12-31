package info.eliumontoyasadec.cryptotracker.ui.admin

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import info.eliumontoyasadec.cryptotracker.data.seed.DeleteRequest
import info.eliumontoyasadec.cryptotracker.data.seed.DeleteResult
import info.eliumontoyasadec.cryptotracker.ui.shell.LocalAppDeps
import kotlinx.coroutines.launch

@Composable
fun DeleteDataScreen(
    onClose: () -> Unit
) {
    val deps = LocalAppDeps.current
    val scope = rememberCoroutineScope()

    var all by remember { mutableStateOf(false) }

    var cryptos by remember { mutableStateOf(true) }
    var wallets by remember { mutableStateOf(true) }
    var fiat by remember { mutableStateOf(true) }
    var movements by remember { mutableStateOf(true) }
    var holdings by remember { mutableStateOf(true) }
    var portfolio by remember { mutableStateOf(true) }

    var loading by remember { mutableStateOf(false) }

    var showConfirm by remember { mutableStateOf(false) }
    var pendingRequest by remember { mutableStateOf<DeleteRequest?>(null) }

    // Modal final (resultado)
    var showResult by remember { mutableStateOf(false) }
    var lastResult by remember { mutableStateOf<DeleteResult?>(null) }
    var lastError by remember { mutableStateOf<String?>(null) }

    fun buildRequest(): DeleteRequest = DeleteRequest(
        all = all,
        wallets = if (all) true else wallets,
        cryptos = if (all) true else cryptos,
        fiat = if (all) true else fiat,
        movements = if (all) true else movements,
        holdings = if (all) true else holdings,
        portfolio = if (all) true else portfolio
    )

    fun openConfirm() {
        pendingRequest = buildRequest()
        showConfirm = true
    }

    fun confirmDelete() {
        val req = pendingRequest ?: return

        // cierra confirmación y ejecuta
        showConfirm = false
        loading = true

        scope.launch {
            try {
                val res = deps.databaseWiper.wipe(req)
                lastResult = res
                lastError = null
            } catch (t: Throwable) {
                lastResult = null
                lastError = t.message ?: "Fallo desconocido"
            } finally {
                loading = false
                pendingRequest = null
                showResult = true
            }
        }
    }

    val anySelected = all || cryptos || wallets || fiat || movements || holdings || portfolio

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Icon(
            imageVector = Icons.Filled.Warning,
            contentDescription = null,
            modifier = Modifier.size(40.dp)
        )

        Text("Eliminar Datos Existentes", style = MaterialTheme.typography.headlineSmall)
        Text(
            "Acción irreversible. Selecciona las tablas a eliminar y confirma.",
            style = MaterialTheme.typography.bodySmall
        )

        ElevatedCard {
            Column(
                modifier = Modifier.padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                ToggleRow(title = "Eliminar todo",
                    subtitle = "Borra toda la base de datos",
                    checked = all,
                    enabled = !loading,
                    onCheckedChange = {
                        all = it
                        if (it) {
                            cryptos = true
                            wallets = true
                            fiat = true
                            movements = true
                            holdings = true
                            portfolio = true
                        }
                    })

                HorizontalDivider()

                ToggleRow(title = "Movimientos",
                    subtitle = "Borra movimientos registrados",
                    checked = movements,
                    enabled = !loading && !all,
                    onCheckedChange = { movements = it })
                HorizontalDivider()

                ToggleRow(title = "Holdings",
                    subtitle = "Borra holdings calculados",
                    checked = holdings,
                    enabled = !loading && !all,
                    onCheckedChange = { holdings = it })
                HorizontalDivider()

                ToggleRow(title = "Carteras",
                    subtitle = "Borra carteras",
                    checked = wallets,
                    enabled = !loading && !all,
                    onCheckedChange = { wallets = it })
                HorizontalDivider()

                ToggleRow(title = "Portafolio",
                    subtitle = "Borra portafolio(s)",
                    checked = portfolio,
                    enabled = !loading && !all,
                    onCheckedChange = { portfolio = it })
                HorizontalDivider()

                ToggleRow(title = "Cryptos",
                    subtitle = "Borra catálogo de cryptos",
                    checked = cryptos,
                    enabled = !loading && !all,
                    onCheckedChange = { cryptos = it })
                HorizontalDivider()

                ToggleRow(title = "Monedas FIAT",
                    subtitle = "Borra catálogo FIAT",
                    checked = fiat,
                    enabled = !loading && !all,
                    onCheckedChange = { fiat = it })
            }
        }

        ElevatedCard {
            Column(
                modifier = Modifier.padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        onClick = onClose, enabled = !loading, modifier = Modifier.weight(1f)
                    ) {
                        Text("Cerrar")
                    }

                    Button(
                        onClick = { openConfirm() },
                        enabled = !loading && anySelected,
                        modifier = Modifier.weight(1f)
                    ) {
                        if (loading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(18.dp), strokeWidth = 2.dp
                            )
                            Spacer(Modifier.size(8.dp))
                        }
                        Text("Borrar datos")
                    }
                }
            }
        }

        Spacer(Modifier.weight(1f))

        Row(
            modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End
        ) {
            OutlinedButton(
                onClick = onClose, enabled = !loading
            ) {
                Text("Cerrar")
            }
        }
    }

    // Modal de confirmación
    if (showConfirm) {
        val req = pendingRequest

        AlertDialog(onDismissRequest = { if (!loading) showConfirm = false },
            title = { Text("Confirmar eliminación") },
            text = {
                Text(buildString {
                    appendLine("Se eliminarán datos de forma permanente:")
                    if (req?.all == true) {
                        appendLine("• TODA LA BASE DE DATOS")
                    } else {
                        if (req?.movements == true) appendLine("• Movimientos")
                        if (req?.holdings == true) appendLine("• Holdings")
                        if (req?.wallets == true) appendLine("• Carteras")
                        if (req?.portfolio == true) appendLine("• Portafolio")
                        if (req?.cryptos == true) appendLine("• Cryptos")
                        if (req?.fiat == true) appendLine("• FIAT")
                    }
                    appendLine()
                    append("¿Deseas continuar?")
                })
            },
            confirmButton = {
                Button(
                    onClick = { confirmDelete() }, enabled = !loading
                ) { Text("Confirmar") }
            },
            dismissButton = {
                OutlinedButton(
                    onClick = { showConfirm = false }, enabled = !loading
                ) { Text("Cancelar") }
            })
    }

    // Modal final con totales borrados
    if (showResult) {
        val error = lastError
        val res = lastResult

        AlertDialog(onDismissRequest = { /* controlado por botón */ },
            title = { Text(if (error == null) "Eliminación completada" else "No se pudo eliminar") },
            text = {
                if (error != null) {
                    Text("Error: $error")
                } else if (res != null) {
                    Text(buildString {
                        appendLine("Totales eliminados por tabla:")
                        appendLine("• Portafolio: ${res.portfoliosDeletedCount}")
                        appendLine("• Carteras: ${res.walletsDeletedCount}")
                        appendLine("• Holdings: ${res.holdingsDeletedCount}")
                        appendLine("• Movimientos: ${res.movementsDeletedCount}")
                        appendLine("• Cryptos: ${res.cryptosDeletedCount}")
                        appendLine("• FIAT: ${res.fiatDeletedCount}")
                    })
                } else {
                    Text("Sin detalles disponibles.")
                }
            },
            confirmButton = {
                Button(onClick = {
                    showResult = false
                    // si fue éxito, volvemos a Setup Inicial
                    if (error == null) onClose()
                }) { Text("OK") }
            })
    }
}

@Composable
private fun ToggleRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    enabled: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween
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