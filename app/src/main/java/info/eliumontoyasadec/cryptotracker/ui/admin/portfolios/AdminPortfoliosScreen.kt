package info.eliumontoyasadec.cryptotracker.ui.admin.portfolios

import androidx.compose.foundation.clickable
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
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
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
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import info.eliumontoyasadec.cryptotracker.domain.model.Portfolio
import info.eliumontoyasadec.cryptotracker.ui.shell.LocalAppDeps
import androidx.compose.ui.platform.testTag

private const val TAG_ADD_FAB = "admin_portfolios_add_fab"
private const val TAG_LIST = "admin_portfolios_list"
private const val TAG_EMPTY = "admin_portfolios_empty"

private const val TAG_FORM_NAME = "admin_portfolios_form_name"
private const val TAG_FORM_DESC = "admin_portfolios_form_desc"
private const val TAG_FORM_DEFAULT = "admin_portfolios_form_default"
private const val TAG_FORM_SAVE = "admin_portfolios_form_save"

private fun tagRow(id: Long) = "admin_portfolios_row_$id"
private fun tagDelete(id: Long) = "admin_portfolios_delete_$id"
private fun tagEdit(id: Long) = "admin_portfolios_edit_$id"
private fun tagMakeDefault(id: Long) = "admin_portfolios_make_default_$id"
private fun tagDefaultBadge(id: Long) = "admin_portfolios_default_badge_$id"
@Composable
fun AdminPortfoliosScreen(
) {
    val deps = LocalAppDeps.current
    val vm: AdminPortfoliosViewModel = viewModel(
        factory = AdminPortfoliosViewModelFactory(deps.portfolioRepository)
    )

    val state = vm.state

    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(Unit) {
        vm.load()
    }

    // Si hay error en state, lo mostramos como snackbar (y no “se pierde”)
    LaunchedEffect(state.error) {
        val msg = state.error ?: return@LaunchedEffect
        snackbarHostState.showSnackbar(msg)
    }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(
                modifier = Modifier.testTag(TAG_ADD_FAB),

                onClick = {
                    vm.openCreate()
                }
            ) {
                Icon(Icons.Filled.Add, contentDescription = "Crear portafolio")
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (state.loading) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center
                ) {
                    CircularProgressIndicator()
                }
            }

            if (state.items.isEmpty() && !state.loading) {
                Text(
                    "No hay portafolios todavía. Crea el primero con el botón +.",
                    modifier = Modifier.testTag(TAG_EMPTY),

                    style = MaterialTheme.typography.bodyMedium
                )
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.fillMaxSize().testTag(TAG_LIST),
                ) {
                    items(state.items, key = { it.portfolioId }) { p ->
                        PortfolioRow(
                            portfolio = p,
                            onEdit = {
                                vm.openEdit(p)
                            },
                            onDelete = {
                                vm.requestDelete(p)
                            },
                            onMakeDefault = { vm.setDefault(p.portfolioId) }
                        )
                    }
                }
            }
        }
    }

    if (state.showForm) {
        PortfolioFormDialog(
            initial = state.editing,
            onDismiss = { vm.dismissForm() },
            onSave = { name, description, makeDefault ->
                vm.save(name = name, description = description, makeDefault = makeDefault)
            }
        )
    }

    if (state.showDeleteConfirm) {
        val target = state.pendingDelete
        AlertDialog(
            onDismissRequest = {
                vm.cancelDelete()
            },
            title = { Text("Eliminar portafolio") },
            text = {
                Text("¿Seguro que deseas eliminar “${target?.name ?: ""}”? Esta acción no se puede deshacer.")
            },
            confirmButton = {
                Button(
                    onClick = {
                        vm.confirmDelete()
                    }
                ) { Text("Eliminar") }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        vm.cancelDelete()
                    }
                ) { Text("Cancelar") }
            }
        )
    }
}

@Composable
private fun PortfolioRow(
    portfolio: Portfolio,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onMakeDefault: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag(tagRow(portfolio.portfolioId))
            .clickable { onEdit() }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(portfolio.name, style = MaterialTheme.typography.titleMedium)
                    if (portfolio.isDefault) {
                        Spacer(Modifier.padding(horizontal = 6.dp))
                        Text("• Default",
                            modifier = Modifier.testTag(tagDefaultBadge(portfolio.portfolioId)),
                            style = MaterialTheme.typography.bodySmall)
                    }
                }

                portfolio.description?.takeIf { it.isNotBlank() }?.let {
                    Spacer(Modifier.height(4.dp))
                    Text(it, style = MaterialTheme.typography.bodySmall)
                }
            }
            if (!portfolio.isDefault) {
                TextButton(  modifier = Modifier.testTag(tagMakeDefault(portfolio.portfolioId)),
                    onClick = onMakeDefault) {
                    Text("Hacer default")
                }
            }

            IconButton(  modifier = Modifier.testTag(tagEdit(portfolio.portfolioId)),
                onClick = onEdit) {
                Icon(Icons.Filled.Edit, contentDescription = "Editar")
            }
            IconButton(  modifier = Modifier.testTag(tagDelete(portfolio.portfolioId)),
                onClick = onDelete) {
                Icon(Icons.Filled.Delete, contentDescription = "Eliminar")
            }
        }
    }
}

@Composable
private fun PortfolioFormDialog(
    initial: Portfolio?,
    onDismiss: () -> Unit,
    onSave: (name: String, description: String?, makeDefault: Boolean) -> Unit
) {
    var name by remember(initial) { mutableStateOf(initial?.name.orEmpty()) }
    var description by remember(initial) { mutableStateOf(initial?.description.orEmpty()) }
    var makeDefault by remember(initial) { mutableStateOf(initial?.isDefault ?: false) }

    val isValid = name.trim().isNotEmpty()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (initial == null) "Crear portafolio" else "Editar portafolio") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Nombre") },
                    singleLine = true,
                    isError = name.isNotBlank() && !isValid,
                    modifier = Modifier.fillMaxWidth().testTag(TAG_FORM_NAME),
                )

                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Descripción (opcional)") },
                    modifier = Modifier.fillMaxWidth().testTag(TAG_FORM_DESC),
                    minLines = 2
                )

                HorizontalDivider()

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Checkbox(
                        modifier = Modifier.testTag(TAG_FORM_DEFAULT),
                        checked = makeDefault,
                        onCheckedChange = { makeDefault = it }
                    )
                    Spacer(Modifier.padding(horizontal = 6.dp))
                    Text("Marcar como portafolio default")
                }
            }
        },
        confirmButton = {
            Button(  modifier = Modifier.testTag(TAG_FORM_SAVE),

                onClick = {
                    val desc = description.trim().takeIf { it.isNotEmpty() }
                    onSave(name.trim(), desc, makeDefault)
                },
                enabled = isValid
            ) { Text("Guardar") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancelar") }
        }
    )
}