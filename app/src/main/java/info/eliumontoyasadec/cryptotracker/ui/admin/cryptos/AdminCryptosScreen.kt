package info.eliumontoyasadec.cryptotracker.ui.admin.cryptos

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Card
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import info.eliumontoyasadec.cryptotracker.ui.shell.LocalAppDeps

@Composable
fun AdminCryptosScreen(
 ) {
    val deps = LocalAppDeps.current

    val vm: AdminCryptosViewModel = viewModel(factory = AdminCryptosViewModelFactory(cryptoRepository= deps.cryptoRepository))
    var state by remember { mutableStateOf(vm.state) }

    val snackbarHostState = remember { SnackbarHostState() }

    // “binding” simple: recomponer cuando cambie la state interna
    LaunchedEffect(Unit) {
        vm.load()
    }
    LaunchedEffect(vm.state) {
        state = vm.state
    }

    // Snackbar del último evento
    LaunchedEffect(state.lastActionMessage) {
        val msg = state.lastActionMessage ?: return@LaunchedEffect
        snackbarHostState.showSnackbar(msg)
        vm.consumeLastActionMessage()
    }

    Scaffold(

        floatingActionButton = {
            FloatingActionButton(onClick = { vm.openCreate(); state = vm.state }) {
                Icon(Icons.Default.Add, contentDescription = "Agregar crypto")
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (state.loading) {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
            }

            state.error?.let {
                AssistChip(
                    onClick = { /* nada */ },
                    label = { Text(it) }
                )
            }

            if (!state.loading && state.items.isEmpty()) {
                Text(
                    "No hay cryptos registradas.",
                    style = MaterialTheme.typography.bodyLarge
                )
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(state.items, key = { it.symbol }) { item ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { vm.openEdit(item); state = vm.state }
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(item.symbol, fontWeight = FontWeight.SemiBold)
                                    Text(item.name, style = MaterialTheme.typography.bodyMedium)
                                }

                                val status = if (item.isActive) "Activa" else "Inactiva"
                                AssistChip(
                                    onClick = { /* read-only */ },
                                    label = { Text(status) }
                                )

                                Spacer(Modifier.width(8.dp))

                                TextButton(onClick = { vm.requestDelete(item.symbol); state = vm.state }) {
                                    Text("Eliminar")
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Form dialog
    if (state.showForm) {
        AlertDialog(
            onDismissRequest = { vm.dismissForm(); state = vm.state },
            confirmButton = {
                TextButton(onClick = { vm.save(); state = vm.state }) { Text("Guardar") }
            },
            dismissButton = {
                TextButton(onClick = { vm.dismissForm(); state = vm.state }) { Text("Cancelar") }
            },
            title = { Text(if (state.isEditing) "Editar crypto" else "Crear crypto") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(
                        value = state.draftSymbol,
                        onValueChange = { vm.onDraftSymbolChange(it); state = vm.state },
                        label = { Text("Símbolo") },
                        enabled = !state.isEditing,
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = state.draftName,
                        onValueChange = { vm.onDraftNameChange(it); state = vm.state },
                        label = { Text("Nombre") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Switch(
                            checked = state.draftActive,
                            onCheckedChange = { vm.onDraftActiveChange(it); state = vm.state }
                        )
                        Text("Activa")
                    }

                    state.error?.let {
                        Text(it, color = MaterialTheme.colorScheme.error)
                    }
                }
            }
        )
    }

    // Delete confirm dialog
    state.pendingDeleteSymbol?.let { symbol ->
        AlertDialog(
            onDismissRequest = { vm.cancelDelete(); state = vm.state },
            confirmButton = {
                TextButton(onClick = { vm.confirmDelete(); state = vm.state }) {
                    Text("Eliminar")
                }
            },
            dismissButton = {
                TextButton(onClick = { vm.cancelDelete(); state = vm.state }) { Text("Cancelar") }
            },
            title = { Text("Confirmar eliminación") },
            text = {
                Text("Se eliminará la crypto \"$symbol\". Si está relacionada con movimientos/holdings, es posible que falle.")
            }
        )
    }
}