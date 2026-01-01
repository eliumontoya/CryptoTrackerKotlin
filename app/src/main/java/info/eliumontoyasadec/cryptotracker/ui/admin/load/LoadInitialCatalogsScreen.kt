package info.eliumontoyasadec.cryptotracker.ui.admin.load

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Download
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

/**
 * Opción A (igual que SetupInicial):
 * - No Scaffold / No TopAppBar aquí.
 * - AppShell ya pone el TopAppBar y el menú/back según corresponda.
 */
@Composable
fun LoadInitialCatalogsScreen(
    onClose: () -> Unit
) {
    val deps = LocalAppDeps.current

    val vm: LoadInitialCatalogsViewModel = viewModel(
        factory = LoadInitialCatalogsViewModelFactory(deps.catalogSeeder)
    )

    var state by remember { mutableStateOf(vm.state) }
    LaunchedEffect(vm.state) { state = vm.state }

    val canStart = !state.loading && (state.wallets || state.cryptos || state.fiat || state.syncManual)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
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

        state.error?.let {
            Text(it, style = MaterialTheme.typography.bodySmall)
        }

        ElevatedCard {
            Column(
                modifier = Modifier.padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                CatalogToggleRow(
                    title = "Carteras",
                    subtitle = if (state.walletsAlready) "Ya existe data (bloqueado)" else "Crea portafolio default y wallets base",
                    checked = state.wallets,
                    enabled = !state.walletsAlready && !state.loading,
                    onCheckedChange = {
                        vm.dispatch(LoadInitialCatalogsAction.ToggleWallets(it))
                        state = vm.state
                    }
                )

                HorizontalDivider()

                CatalogToggleRow(
                    title = "Cryptos",
                    subtitle = if (state.cryptosAlready) "Ya existe data (bloqueado)" else "Crea catálogo de cryptos (BTC, ETH, SOL, USDT...)",
                    checked = state.cryptos,
                    enabled = !state.cryptosAlready && !state.loading,
                    onCheckedChange = {
                        vm.dispatch(LoadInitialCatalogsAction.ToggleCryptos(it))
                        state = vm.state
                    }
                )

                HorizontalDivider()

                CatalogToggleRow(
                    title = "FIAT",
                    subtitle = if (state.fiatAlready) "Ya existe data (bloqueado)" else "Crea catálogo fiat (USD, MXN, EUR...)",
                    checked = state.fiat,
                    enabled = !state.fiatAlready && !state.loading,
                    onCheckedChange = {
                        vm.dispatch(LoadInitialCatalogsAction.ToggleFiat(it))
                        state = vm.state
                    }
                )

                HorizontalDivider()

                CatalogToggleRow(
                    title = "Sync Manual",
                    subtitle = "Aplica configuración base (placeholder listo para crecer)",
                    checked = state.syncManual,
                    enabled = false, // lo mantengo igual que tu implementación actual
                    onCheckedChange = {
                        vm.dispatch(LoadInitialCatalogsAction.ToggleSyncManual(it))
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
                Button(
                    onClick = {
                        vm.dispatch(LoadInitialCatalogsAction.RequestSeed)
                        state = vm.state
                    },
                    enabled = canStart,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    if (state.loading) {
                        CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                        Spacer(Modifier.padding(4.dp))
                    }
                    Text("Iniciar Carga")
                }

                state.lastResult?.let {
                    Text(it, style = MaterialTheme.typography.bodySmall)
                }
            }
        }

        Spacer(Modifier.weight(1f))

        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
            OutlinedButton(onClick = onClose) { Text("Cerrar") }
        }
    }

    if (state.showConfirm) {
        AlertDialog(
            onDismissRequest = {
                vm.dispatch(LoadInitialCatalogsAction.CancelSeed)
                state = vm.state
            },
            title = { Text("Confirmar carga") },
            text = { Text(state.confirmText) },
            confirmButton = {
                Button(onClick = {
                    vm.dispatch(LoadInitialCatalogsAction.ConfirmSeed)
                    state = vm.state
                }) { Text("Confirmar") }
            },
            dismissButton = {
                OutlinedButton(onClick = {
                    vm.dispatch(LoadInitialCatalogsAction.CancelSeed)
                    state = vm.state
                }) { Text("Cancelar") }
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