package info.eliumontoyasadec.cryptotracker.ui.admin.delete

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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import info.eliumontoyasadec.cryptotracker.ui.shell.LocalAppDeps

@Composable
fun DeleteDataScreen(
    onClose: () -> Unit
) {
    val deps = LocalAppDeps.current

    val vm: DeleteDataViewModel = viewModel(
        factory = DeleteDataViewModelFactory(deps.databaseWiper)
    )

    var state by remember { mutableStateOf(vm.state) }
    LaunchedEffect(vm.state) { state = vm.state }

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
                ToggleRow(
                    title = "Eliminar todo",
                    subtitle = "Borra toda la base de datos",
                    checked = state.all,
                    enabled = !state.loading,
                    onCheckedChange = {
                        vm.dispatch(DeleteDataAction.ToggleAll(it))
                        state = vm.state
                    }
                )

                HorizontalDivider()

                ToggleRow(
                    title = "Movimientos",
                    subtitle = "Borra movimientos registrados",
                    checked = state.movements,
                    enabled = !state.loading && !state.all,
                    onCheckedChange = {
                        vm.dispatch(DeleteDataAction.ToggleMovements(it))
                        state = vm.state
                    }
                )

                HorizontalDivider()

                ToggleRow(
                    title = "Holdings",
                    subtitle = "Borra holdings calculados",
                    checked = state.holdings,
                    enabled = !state.loading && !state.all,
                    onCheckedChange = {
                        vm.dispatch(DeleteDataAction.ToggleHoldings(it))
                        state = vm.state
                    }
                )

                HorizontalDivider()

                ToggleRow(
                    title = "Carteras",
                    subtitle = "Borra carteras",
                    checked = state.wallets,
                    enabled = !state.loading && !state.all,
                    onCheckedChange = {
                        vm.dispatch(DeleteDataAction.ToggleWallets(it))
                        state = vm.state
                    }
                )

                HorizontalDivider()

                ToggleRow(
                    title = "Portafolio",
                    subtitle = "Borra portafolio(s)",
                    checked = state.portfolio,
                    enabled = !state.loading && !state.all,
                    onCheckedChange = {
                        vm.dispatch(DeleteDataAction.TogglePortfolio(it))
                        state = vm.state
                    }
                )

                HorizontalDivider()

                ToggleRow(
                    title = "Cryptos",
                    subtitle = "Borra catálogo de cryptos",
                    checked = state.cryptos,
                    enabled = !state.loading && !state.all,
                    onCheckedChange = {
                        vm.dispatch(DeleteDataAction.ToggleCryptos(it))
                        state = vm.state
                    }
                )

                HorizontalDivider()

                ToggleRow(
                    title = "Monedas FIAT",
                    subtitle = "Borra catálogo FIAT",
                    checked = state.fiat,
                    enabled = !state.loading && !state.all,
                    onCheckedChange = {
                        vm.dispatch(DeleteDataAction.ToggleFiat(it))
                        state = vm.state
                    }
                )
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
                        onClick = onClose,
                        enabled = !state.loading,
                        modifier = Modifier.weight(1f)
                    ) { Text("Cerrar") }

                    Button(
                        onClick = {
                            vm.dispatch(DeleteDataAction.RequestDelete)
                            state = vm.state
                        },
                        enabled = !state.loading && state.anySelected,
                        modifier = Modifier.weight(1f)
                    ) {
                        if (state.loading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(18.dp),
                                strokeWidth = 2.dp
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
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End
        ) {
            OutlinedButton(
                onClick = onClose,
                enabled = !state.loading
            ) { Text("Cerrar") }
        }
    }

    // Confirmación
    if (state.showConfirm) {
        val req = state.pendingRequest

        AlertDialog(
            onDismissRequest = { vm.dispatch(DeleteDataAction.CancelConfirm); state = vm.state },
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
                    onClick = { vm.dispatch(DeleteDataAction.ConfirmDelete); state = vm.state },
                    enabled = !state.loading
                ) { Text("Confirmar") }
            },
            dismissButton = {
                OutlinedButton(
                    onClick = { vm.dispatch(DeleteDataAction.CancelConfirm); state = vm.state },
                    enabled = !state.loading
                ) { Text("Cancelar") }
            }
        )
    }

    // Resultado
    if (state.showResult) {
        val error = state.lastError
        val res = state.lastResult

        AlertDialog(
            onDismissRequest = { /* controlado por botón */ },
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
                    val wasSuccess = (error == null)
                    vm.dispatch(DeleteDataAction.DismissResult)
                    state = vm.state
                    if (wasSuccess) onClose()
                }) { Text("OK") }
            }
        )
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