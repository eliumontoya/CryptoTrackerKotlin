package info.eliumontoyasadec.cryptotracker.ui.admin.fiat

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import info.eliumontoyasadec.cryptotracker.ui.shell.LocalAppDeps

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminFiatScreen(
    onClose: () -> Unit
) {
    val deps = LocalAppDeps.current
    val vm: AdminFiatViewModel = viewModel(factory = AdminFiatViewModelFactory(deps.fiatRepository))
    val state = vm.state

    LaunchedEffect(Unit) { vm.load() }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Administración · Fiat") },
                navigationIcon = {
                    TextButton(onClick = onClose) { Text("Cerrar") }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { vm.openCreate() }) {
                Icon(Icons.Default.Add, contentDescription = "Crear fiat")
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .padding(16.dp)
        ) {

            state.error?.let {
                Text(
                    text = it,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodyMedium
                )
                Spacer(Modifier.height(8.dp))
            }

            if (state.items.isEmpty() && !state.loading) {
                Text("No hay monedas fiat registradas.")
                Spacer(Modifier.height(8.dp))
            }

            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(state.items, key = { it.code }) { item ->
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "${item.code} · ${item.name}",
                                    style = MaterialTheme.typography.titleMedium
                                )
                                Text(
                                    text = "Símbolo: ${item.symbol}",
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            }

                            Row {
                                IconButton(onClick = { vm.openEdit(item) }) {
                                    Icon(Icons.Default.Edit, contentDescription = "Editar")
                                }
                                IconButton(onClick = { vm.requestDelete(item) }) {
                                    Icon(Icons.Default.Delete, contentDescription = "Eliminar")
                                }
                            }
                        }
                    }
                }
            }
        }

        if (state.showForm) {
            FiatFormDialog(
                editingCodeLocked = state.editing != null,
                initialCode = state.editing?.code.orEmpty(),
                initialName = state.editing?.name.orEmpty(),
                initialSymbol = state.editing?.symbol.orEmpty(),
                onDismiss = { vm.dismissForm() },
                onSave = { code, name, symbol ->
                    vm.save(code, name, symbol ?: "")
                }            )
        }

        if (state.showDeleteConfirm) {
            val target = state.pendingDelete
            AlertDialog(
                onDismissRequest = { vm.cancelDelete() },
                title = { Text("Confirmar eliminación") },
                text = { Text("Se eliminará: ${target?.code ?: "—"} · ${target?.name ?: ""}") },
                confirmButton = {
                    Button(onClick = { vm.confirmDelete() }) { Text("Eliminar") }
                },
                dismissButton = {
                    TextButton(onClick = { vm.cancelDelete() }) { Text("Cancelar") }
                }
            )
        }
    }
}

@Composable
private fun FiatFormDialog(
    editingCodeLocked: Boolean,
    initialCode: String,
    initialName: String,
    initialSymbol: String,
    onDismiss: () -> Unit,
    onSave: (code: String, name: String, symbol: String?) -> Unit
) {
    var code by remember { mutableStateOf(initialCode) }
    var name by remember { mutableStateOf(initialName) }
    var symbol by remember { mutableStateOf(initialSymbol) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (editingCodeLocked) "Editar fiat" else "Crear fiat") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = code,
                    onValueChange = { if (!editingCodeLocked) code = it },
                    enabled = !editingCodeLocked,
                    label = { Text("Código (PK)") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Nombre") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                OutlinedTextField(
                    value = symbol,
                    onValueChange = { symbol = it },
                    label = { Text("Símbolo (opcional)") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
            }
        },
        confirmButton = {
            Button(onClick = { onSave(code, name, symbol) }) { Text("Guardar") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancelar") }
        }
    )
}