@file:OptIn(ExperimentalMaterial3Api::class)

package info.eliumontoyasadec.cryptotracker.ui.screens.movements

import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import info.eliumontoyasadec.cryptotracker.domain.model.Wallet

enum class MovementMode { IN, OUT, BETWEEN, SWAP }

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
    val walletId: Long?,
    val walletName: String,
    val crypto: CryptoFilter,
    val headline: String,
    val details: String
)

data class MovementsUiState(
    val wallets: List<Wallet> = emptyList(),
    val selectedWalletId: Long? = null, // null = Todas
    val selectedCrypto: CryptoFilter = CryptoFilter.ALL,
    val rows: List<MovementRow> = emptyList(),
    val filteredRows: List<MovementRow> = emptyList(),
    val movementForm: MovementFormState? = null,
    val swapForm: SwapFormState? = null,
    val pendingDeleteId: String? = null,
    val error: String? = null
)

data class MovementFormState(
    val mode: MovementFormMode,
    val draft: MovementDraft
)

data class SwapFormState(
    val draft: SwapDraft
)

@Composable
fun MovementsScreen(
    title: String,
    state: MovementsUiState,
    onSelectWallet: (Long?) -> Unit,
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
    onSwapSave: () -> Unit,
    onClearError: () -> Unit
) {
    // Si quieres mostrar error sin crashear:
    LaunchedEffect(state.error) {
        if (state.error != null) onClearError()
    }

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
                        row.walletName.lowercase().contains(q)
            }
        }

        val hasActiveFilters =
            (state.selectedWalletId != null) ||
                    (state.selectedCrypto != CryptoFilter.ALL) ||
                    (searchQuery.isNotBlank())

        Text(title, style = androidx.compose.material3.MaterialTheme.typography.headlineSmall)

        // Filters
        ElevatedCard {
            Column(
                modifier = Modifier.padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text("Filtros", style = androidx.compose.material3.MaterialTheme.typography.titleMedium)
                HorizontalDivider()

                Text("Cartera", style = androidx.compose.material3.MaterialTheme.typography.labelLarge)

                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    item {
                        FilterChip(
                            selected = state.selectedWalletId == null,
                            onClick = { onSelectWallet(null) },
                            label = { Text("Todas") }
                        )
                    }
                    items(state.wallets, key = { it.walletId }) { w ->
                        FilterChip(
                            selected = state.selectedWalletId == w.walletId,
                            onClick = { onSelectWallet(w.walletId) },
                            label = { Text(w.name) }
                        )
                    }
                }

                Spacer(Modifier.height(6.dp))

                Text("Crypto", style = androidx.compose.material3.MaterialTheme.typography.labelLarge)

                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(CryptoFilter.entries) { c ->
                        FilterChip(
                            selected = state.selectedCrypto == c,
                            onClick = { onSelectCrypto(c) },
                            label = { Text(c.label) }
                        )
                    }
                }
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
                    Text("Movimientos", style = androidx.compose.material3.MaterialTheme.typography.titleMedium)
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text("${shownRows.size}", style = androidx.compose.material3.MaterialTheme.typography.labelLarge)
                        OutlinedButton(
                            onClick = onCreate,
                            modifier = Modifier.testTag(MovementTags.AddButton)
                        ) { Text("Nuevo") }
                    }
                }
                HorizontalDivider()

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    val parts = buildList {
                        val w = state.wallets.firstOrNull { it.walletId == state.selectedWalletId }
                        if (w != null) add(w.name)
                        if (state.selectedCrypto != CryptoFilter.ALL) add(state.selectedCrypto.label)
                        if (searchQuery.isNotBlank()) add("Buscar: $searchQuery")
                    }

                    Text(
                        text = if (parts.isEmpty()) "Filtros activos: Ninguno" else "Filtros activos: ${parts.joinToString(" · ")}",
                        style = androidx.compose.material3.MaterialTheme.typography.bodySmall,
                        modifier = Modifier.weight(1f)
                    )

                    if (hasActiveFilters) {
                        TextButton(
                            onClick = {
                                searchQuery = ""
                                onSelectWallet(null)
                                onSelectCrypto(CryptoFilter.ALL)
                            }
                        ) { Text("Limpiar") }
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
                        style = androidx.compose.material3.MaterialTheme.typography.bodyMedium
                    )
                } else {
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(shownRows, key = { it.id }) { row ->
                            MovementRowCard(
                                row = row,
                                onEdit = { onEdit(row) },
                                onDelete = { onRequestDelete(row) }
                            )
                        }
                    }
                }
            }
        }
    }

    // Delete modal
    val pendingId = state.pendingDeleteId
    if (pendingId != null) {
        AlertDialog(
            onDismissRequest = onCancelDelete,
            confirmButton = { TextButton(onClick = { onConfirmDelete(pendingId) }) { Text("Borrar") } },
            dismissButton = { TextButton(onClick = onCancelDelete) { Text("Cancelar") } },
            title = { Text("Borrar movimiento") },
            text = { Text("¿Seguro que deseas borrar este movimiento?") }
        )
    }

    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    // Form sheets
    state.movementForm?.let { form ->
        ModalBottomSheet(
            onDismissRequest = onDismissForms,
            sheetState = sheetState
        ) {
            val mv = remember(form.mode, form.draft) {
                MovementFormModelView(
                    initialMode = form.mode,
                    initialDraft = form.draft,
                    onCancelExternal = onDismissForms,
                    onDraftChangeExternal = onMovementDraftChange,
                    onSaveExternal = onMovementSave
                )
            }
            MovementFormSheetContent(
                state = mv.state,
                mv = mv,
                wallets = state.wallets // ✅ dinámico BD
            )
            Spacer(Modifier.height(12.dp))
            OutlinedButton(
                onClick = onMovementSave,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .testTag(MovementTags.FormSave)
            ) { Text("Guardar") }
            Spacer(Modifier.height(24.dp))
        }
    }

    state.swapForm?.let { form ->
        ModalBottomSheet(
            onDismissRequest = onDismissForms,
            sheetState = sheetState
        ) {
            SwapFormSheetContent(
                draft = form.draft,
                wallets = state.wallets,
                onDraftChange = onSwapDraftChange,
                onCancel = onDismissForms,
                onSave = onSwapSave
            )

        }
    }
}

// Conserva tu Card actual; solo asegúrate de mostrar walletName.
@Composable
private fun MovementRowCard(
    row: MovementRow,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    ElevatedCard(
        modifier = Modifier
            .fillMaxWidth()
            .testTag(MovementTags.swapRow(row.id))

    ) {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(row.headline, style = androidx.compose.material3.MaterialTheme.typography.titleMedium)
            Text("${row.walletName} · ${row.crypto.label} · ${row.dateLabel}", style = androidx.compose.material3.MaterialTheme.typography.bodySmall)
            Text(row.details, style = androidx.compose.material3.MaterialTheme.typography.bodySmall)

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TextButton(onClick = onEdit) { Text("Editar") }
                TextButton(onClick = onDelete) { Text("Borrar") }
            }
        }
    }
}