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
import androidx.compose.material3.Card
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import info.eliumontoyasadec.cryptotracker.ui.shell.LocalAppDeps

object AdminFiatTags {
    const val SCREEN = "admin_fiat_screen"
    const val LIST = "admin_fiat_list"
    const val FAB_ADD = "admin_fiat_add"

    const val LOADING = "admin_fiat_loading"
    const val EMPTY = "admin_fiat_empty"
    const val ERROR = "admin_fiat_error"

    const val FORM_DIALOG = "admin_fiat_form_dialog"
    const val FIELD_CODE = "admin_fiat_field_code"
    const val FIELD_NAME = "admin_fiat_field_name"
    const val FIELD_SYMBOL = "admin_fiat_field_symbol"
    const val BTN_SAVE = "admin_fiat_form_save"
    const val BTN_CANCEL = "admin_fiat_form_cancel"

    const val DELETE_DIALOG = "admin_fiat_delete_dialog"
    const val BTN_DELETE_CONFIRM = "admin_fiat_delete_confirm"
    const val BTN_DELETE_CANCEL = "admin_fiat_delete_cancel"

    fun item(code: String) = "admin_fiat_item_$code"
    fun btnEdit(code: String) = "admin_fiat_edit_$code"
    fun btnDelete(code: String) = "admin_fiat_delete_$code"
}


@Composable
fun AdminFiatScreen(
) {
    val deps = LocalAppDeps.current
    val vm: AdminFiatViewModel = viewModel(factory = AdminFiatViewModelFactory(deps.fiatRepository))
    val state = vm.state

    LaunchedEffect(Unit) { vm.load() }

    Scaffold(

        floatingActionButton = {
            FloatingActionButton(modifier = Modifier.testTag(AdminFiatTags.FAB_ADD),
                onClick = { vm.openCreate() }) {
                Icon(Icons.Default.Add, contentDescription = "Crear fiat")
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .padding(16.dp)
                .testTag(AdminFiatTags.SCREEN)

        ) {

            if (state.error != null) {
                Text(
                    text = state.error,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier
                        .padding(bottom = 8.dp)
                        .testTag(AdminFiatTags.ERROR)
                )
            }

            if (state.loading) {
                LinearProgressIndicator(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag(AdminFiatTags.LOADING)
                )
                Spacer(Modifier.height(12.dp))
            }

            if (!state.loading && state.items.isEmpty()) {
                Text(
                    text = "Aún no hay fiats.",
                    modifier = Modifier
                        .padding(12.dp)
                        .testTag(AdminFiatTags.EMPTY)
                )
            }
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier
                    .fillMaxSize()
                    .testTag(AdminFiatTags.LIST),

                ) {
                items(state.items, key = { it.code }) { item ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag(AdminFiatTags.item(item.code))
                            .semantics(mergeDescendants = true) {}
                    ) {
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
                                IconButton(
                                    modifier = Modifier.testTag(AdminFiatTags.btnEdit(item.code)),

                                    onClick = { vm.openEdit(item) }) {
                                    Icon(Icons.Default.Edit, contentDescription = "Editar")
                                }
                                IconButton(
                                    modifier = Modifier.testTag(AdminFiatTags.btnDelete(item.code)),

                                    onClick = { vm.requestDelete(item) }) {
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
                })
        }

        if (state.showDeleteConfirm) {
            val target = state.pendingDelete
            AlertDialog(
                modifier = Modifier.testTag(AdminFiatTags.DELETE_DIALOG),

                onDismissRequest = { vm.cancelDelete() },
                title = { Text("Confirmar eliminación") },
                text = { Text("Se eliminará: ${target?.code ?: "—"} · ${target?.name ?: ""}") },
                confirmButton = {

                    TextButton(
                        modifier = Modifier.testTag(AdminFiatTags.BTN_DELETE_CONFIRM),

                        onClick = { vm.confirmDelete() }) { Text("Eliminar") }
                },
                dismissButton = {
                    TextButton(
                        modifier = Modifier.testTag(AdminFiatTags.BTN_DELETE_CANCEL),

                        onClick = { vm.cancelDelete() }) { Text("Cancelar") }
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
        modifier = Modifier.testTag(AdminFiatTags.FORM_DIALOG),

        onDismissRequest = onDismiss,
        title = { Text(if (editingCodeLocked) "Editar fiat" else "Crear fiat") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = code,
                    onValueChange = { if (!editingCodeLocked) code = it },
                    enabled = !editingCodeLocked,
                    label = { Text("Código (PK)") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag(AdminFiatTags.FIELD_CODE),

                    singleLine = true
                )
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Nombre") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag(AdminFiatTags.FIELD_NAME),
                    singleLine = true
                )
                OutlinedTextField(
                    value = symbol,
                    onValueChange = { symbol = it },
                    label = { Text("Símbolo (opcional)") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag(AdminFiatTags.FIELD_SYMBOL),
                    singleLine = true
                )
            }
        },
        confirmButton = {
            TextButton(modifier = Modifier.testTag(AdminFiatTags.BTN_SAVE),
                onClick = { onSave(code, name, symbol) }) { Text("Guardar") }
        },
        dismissButton = {
            TextButton(
                modifier = Modifier.testTag(AdminFiatTags.BTN_CANCEL),
                onClick = onDismiss
            ) { Text("Cancelar") }
        }
    )
}