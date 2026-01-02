package info.eliumontoyasadec.cryptotracker.ui.admin.wallets

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
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
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import info.eliumontoyasadec.cryptotracker.domain.model.Wallet
import info.eliumontoyasadec.cryptotracker.ui.shell.LocalAppDeps


@Composable
fun AdminWalletsScreen(
) {
    val deps = LocalAppDeps.current

    val vm: AdminWalletsViewModel = viewModel(
        factory = AdminWalletsViewModelFactory(
            walletRepo = deps.walletRepository,
            portfolioRepo = deps.portfolioRepository
        )
    )

    LaunchedEffect(Unit) { vm.start() }

    val state = vm.state

    Scaffold(

        floatingActionButton = {
            FloatingActionButton(onClick = { vm.openCreate() }) {
                Icon(Icons.Filled.Add, contentDescription = "Crear cartera")
            }
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .padding(16.dp)
        ) {
            if (state.error != null) {
                Text(
                    text = state.error,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
            }

            PortfolioPicker(
                portfolios = state.portfolios,
                selectedId = state.selectedPortfolioId,
                onSelect = { vm.selectPortfolio(it) }
            )

            Spacer(Modifier.height(12.dp))

            if (state.loading) {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                Spacer(Modifier.height(12.dp))
            }
            if (!state.loading && state.items.isEmpty()) {
                Text(
                    text = "Aún no hay carteras para este portafolio.",
                    modifier = Modifier.padding(12.dp)
                )
            }
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(state.items, key = { it.walletId }) { item ->
                    WalletRow(
                        item = item,
                        onEdit = { vm.openEdit(item) },
                        onDelete = { vm.deleteWallet(item.walletId) },
                        onMakeMain = { vm.makeMain(item.walletId) }
                    )
                }
            }
        }
    }

    if (state.showEditor) {
        WalletEditorDialog(
            isEdit = state.editorWalletId != null,
            name = state.editorName,
            makeMain = state.editorMakeMain,
            onNameChange = vm::onEditorNameChange,
            onMakeMainChange = vm::onEditorMakeMainChange,
            onDismiss = vm::closeEditor,
            onSave = vm::saveEditor
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PortfolioPicker(
    portfolios: List<info.eliumontoyasadec.cryptotracker.domain.model.Portfolio>,
    selectedId: Long?,
    onSelect: (Long) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    val selected = portfolios.firstOrNull { it.portfolioId == selectedId }

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = !expanded }
    ) {
        OutlinedTextField(
            value = selected?.name ?: "Selecciona portafolio",
            onValueChange = {},
            readOnly = true,
            label = { Text("Portafolio") },
            modifier = Modifier
                .menuAnchor()
                .fillMaxWidth()
        )
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            portfolios.forEach { p ->
                DropdownMenuItem(
                    text = { Text(p.name) },
                    onClick = {
                        expanded = false
                        onSelect(p.portfolioId)
                    }
                )
            }
        }
    }
}

@Composable
private fun WalletRow(
    item: Wallet,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onMakeMain: () -> Unit
) {
    Card {
        Column(Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(item.name, style = MaterialTheme.typography.titleMedium)
                    Text(
                        if (item.isMain) "Principal (isMain)" else "No principal",
                        style = MaterialTheme.typography.bodySmall
                    )
                }

                Text(
                    text = "Editar",
                    modifier = Modifier
                        .padding(start = 12.dp)
                        .clickable { onEdit() }
                )

                Text(
                    text = "Eliminar",
                    modifier = Modifier
                        .padding(start = 12.dp)
                        .clickable { onDelete() }
                )
            }

            if (!item.isMain) {
                Spacer(Modifier.height(8.dp))
                Text(
                    text = "Hacer principal",
                    modifier = Modifier.clickable { onMakeMain() }
                )
            }
        }
    }
}

@Composable
private fun WalletEditorDialog(
    isEdit: Boolean,
    name: String,
    makeMain: Boolean,
    onNameChange: (String) -> Unit,
    onMakeMainChange: (Boolean) -> Unit,
    onDismiss: () -> Unit,
    onSave: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (isEdit) "Editar cartera" else "Crear cartera") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = onNameChange,
                    label = { Text("Nombre") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Switch(checked = makeMain, onCheckedChange = onMakeMainChange)
                    Spacer(Modifier.width(8.dp))
                    Text("Marcar como principal (isMain)")
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onSave) { Text("Guardar") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancelar") }
        }
    )
}