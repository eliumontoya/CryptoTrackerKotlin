@file:OptIn(ExperimentalMaterial3Api::class)

package info.eliumontoyasadec.cryptotracker.ui.screens.movements

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTag
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp


enum class MovementMode { IN, OUT, BETWEEN, SWAP }

enum class WalletFilter(val label: String) {
    ALL("Todas"),
    METAMASK("Metamask"),
    BYBIT("ByBit"),
    PHANTOM("Phantom")
}

enum class CryptoFilter(val label: String) {
    ALL("Todas"),
    BTC("BTC"),
    ETH("ETH"),
    SOL("SOL"),
    ALGO("ALGO"),
    AIXBT("AIXBT")
}

data class MovementRow(
    val id: String,
    val dateLabel: String,
    val wallet: WalletFilter,
    val crypto: CryptoFilter,
    val headline: String,
    val details: String
)

// -------- UI State --------

data class MovementsUiState(
    val selectedWallet: WalletFilter = WalletFilter.ALL,
    val selectedCrypto: CryptoFilter = CryptoFilter.ALL,
    val rows: List<MovementRow> = emptyList(),
    val filteredRows: List<MovementRow> = emptyList(),
    val movementForm: MovementFormState? = null,
    val swapForm: SwapFormState? = null,
    val pendingDeleteId: String? = null
)

data class MovementFormState(
    val mode: MovementFormMode,
    val draft: MovementDraft
)

data class SwapFormState(
    val draft: SwapDraft
)

// -------- Composable --------

@Composable
fun MovementsScreen(
    title: String,
    state: MovementsUiState,
    onSelectWallet: (WalletFilter) -> Unit,
    onSelectCrypto: (CryptoFilter) -> Unit,
    onCreate: () -> Unit,
    onEdit: (MovementRow) -> Unit,
    onRequestDelete: (MovementRow) -> Unit,
    onCancelDelete: () -> Unit,
    onConfirmDelete: (String) -> Unit,
    onDismissForms: () -> Unit,
    onMovementDraftChange: (MovementDraft) -> Unit,
    onMovementSave: () -> Unit,
    onSwapDraftChange: (SwapDraft) -> Unit,
    onSwapSave: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .testTag(MovementTags.Screen)

            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        var searchQuery by remember { mutableStateOf("") }
        val q = searchQuery.trim().lowercase()
        val shownRows = if (q.isBlank()) {
            state.filteredRows
        } else {
            state.filteredRows.filter { row ->
                row.headline.lowercase().contains(q) ||
                        row.details.lowercase().contains(q) ||
                        row.crypto.label.lowercase().contains(q) ||
                        row.wallet.label.lowercase().contains(q)
            }
        }
        val hasActiveFilters =
            (state.selectedWallet != WalletFilter.ALL) ||
                    (state.selectedCrypto != CryptoFilter.ALL) ||
                    (searchQuery.isNotBlank())

        Text(title, style = MaterialTheme.typography.headlineSmall)

        // Filters
        ElevatedCard {
            Column(
                modifier = Modifier.padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text("Filtros", style = MaterialTheme.typography.titleMedium)
                HorizontalDivider()

                Text("Cartera", style = MaterialTheme.typography.labelLarge)
                ChipRow(
                    options = WalletFilter.entries,
                    selected = state.selectedWallet,
                    labelOf = { it.label },
                    onSelect = onSelectWallet
                )

                Spacer(Modifier.height(6.dp))

                Text("Crypto", style = MaterialTheme.typography.labelLarge)
                ChipRow(
                    options = CryptoFilter.entries,
                    selected = state.selectedCrypto,
                    labelOf = { it.label },
                    onSelect = onSelectCrypto
                )
            }
        }

        // List
        ElevatedCard {
            Column(
                modifier = Modifier.padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Movimientos", style = MaterialTheme.typography.titleMedium)
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text("${shownRows.size}", style = MaterialTheme.typography.labelLarge)
                        OutlinedButton(
                            onClick = onCreate, modifier = Modifier.testTag(MovementTags.AddButton)
                        ) { Text("Nuevo") }
                    }
                }
                HorizontalDivider()

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    val parts = buildList {
                        if (state.selectedWallet != WalletFilter.ALL) add(state.selectedWallet.label)
                        if (state.selectedCrypto != CryptoFilter.ALL) add(state.selectedCrypto.label)
                        if (searchQuery.isNotBlank()) add("Buscar: $searchQuery")
                    }

                    Text(
                        text = if (parts.isEmpty()) "Filtros activos: Ninguno" else "Filtros activos: ${
                            parts.joinToString(
                                " · "
                            )
                        }",
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.weight(1f)
                    )

                    if (hasActiveFilters) {
                        TextButton(
                            onClick = {
                                searchQuery = ""
                                onSelectWallet(WalletFilter.ALL)
                                onSelectCrypto(CryptoFilter.ALL)
                            }
                        ) {
                            Text("Limpiar")
                        }
                    }
                }

                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    label = { Text("Buscar") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done)
                )

                if (shownRows.isEmpty()) {
                    Text(
                        text = "No hay movimientos con los filtros actuales.",
                        style = MaterialTheme.typography.bodyMedium
                    )
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag(MovementTags.List),

                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(shownRows, key = { it.id }) { row ->
                            MovementRowItem(
                                row = row,
                                onClick = { onEdit(row) },
                                onEdit = { onEdit(row) },
                                onDelete = { onRequestDelete(row) }
                            )
                            HorizontalDivider()
                        }
                    }
                }
            }
        }

        // Delete confirmation
        if (state.pendingDeleteId != null) {
            AlertDialog(
                modifier = Modifier.testTag(MovementTags.DeleteDialog),

                onDismissRequest = onCancelDelete,
                confirmButton = {
                    TextButton(
                        onClick = {
                            onConfirmDelete(state.pendingDeleteId)
                        }, modifier = Modifier.testTag(MovementTags.DeleteConfirm)
                    ) { Text("Eliminar") }
                },
                dismissButton = {
                    TextButton(
                        onClick = onCancelDelete,
                        modifier = Modifier.testTag(MovementTags.DeleteCancel)
                    ) { Text("Cancelar") }
                },
                title = { Text("Eliminar movimiento") },
                text = { Text("Esta acción no se puede deshacer (fake).") }
            )
        }

        // Bottom sheets
        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

        if (state.movementForm != null) {
            ModalBottomSheet(
                modifier = Modifier.testTag(MovementTags.FormSheetContainer),
                onDismissRequest = onDismissForms,
                sheetState = sheetState
            ) {
                val form = state.movementForm
                val formMv = remember(form.mode, form.draft.id) {
                    MovementFormModelView(
                        initialMode = form.mode,
                        initialDraft = form.draft,
                        onCancelExternal = onDismissForms,
                        onDraftChangeExternal = onMovementDraftChange,
                        onSaveExternal = onMovementSave
                    )
                }

                MovementFormSheetContent(
                    state = formMv.state,
                    mv = formMv
                )
            }
        }

        if (state.swapForm != null) {
            ModalBottomSheet(
                onDismissRequest = onDismissForms,
                sheetState = sheetState
            ) {
                SwapFormSheetContent(
                    draft = state.swapForm.draft,
                    onChange = onSwapDraftChange,
                    onCancel = onDismissForms,
                    onSave = onSwapSave
                )
            }
        }
    }
}

@Composable
private fun <T> ChipRow(
    options: List<T>,
    selected: T,
    labelOf: (T) -> String,
    onSelect: (T) -> Unit
) {
    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        items(options) { opt ->
            FilterChip(
                selected = opt == selected,
                onClick = { onSelect(opt) },
                label = { Text(labelOf(opt)) }
            )
        }
    }
}

@Composable
private fun MovementRowItem(
    row: MovementRow,
    onClick: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()

            .clickable(onClick = onClick)
            .padding(vertical = 6.dp)
            .semantics(mergeDescendants = true) {
                testTag = MovementTags.row(row.id)
            },
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(Modifier.weight(1f)) {
                Text(row.headline, style = MaterialTheme.typography.titleSmall)
                Text(row.dateLabel, style = MaterialTheme.typography.labelMedium)
            }
            Box {
                var expanded by remember { mutableStateOf(false) }

                IconButton(
                    onClick = { expanded = true },
                    modifier = Modifier.testTag(MovementTags.rowMenu(row.id))
                ) {
                    Icon(Icons.Filled.MoreVert, contentDescription = "Más")
                }

                DropdownMenu(
                    expanded = expanded,
                    onDismissRequest = { expanded = false }
                ) {
                    DropdownMenuItem(
                        modifier = Modifier.testTag(MovementTags.rowEdit(row.id)),

                        text = { Text("Editar") },
                        onClick = {
                            expanded = false
                            onEdit()
                        }
                    )
                    DropdownMenuItem(
                        modifier = Modifier.testTag(MovementTags.rowDelete(row.id)),

                        text = { Text("Eliminar") },
                        onClick = {
                            expanded = false
                            onDelete()
                        }
                    )
                }
            }
        }

        Text(
            text = "${row.wallet.label} · ${row.crypto.label}",
            style = MaterialTheme.typography.bodySmall
        )
        Text(row.details, style = MaterialTheme.typography.bodyMedium)
    }
}
