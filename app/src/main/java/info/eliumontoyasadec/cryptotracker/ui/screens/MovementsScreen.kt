@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
package info.eliumontoyasadec.cryptotracker.ui.screens

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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Divider
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import info.eliumontoyasadec.cryptotracker.ui.screens.movements.MovementDraft
import info.eliumontoyasadec.cryptotracker.ui.screens.movements.MovementFormMode
import info.eliumontoyasadec.cryptotracker.ui.screens.movements.MovementFormSheetContent
import info.eliumontoyasadec.cryptotracker.ui.screens.movements.MovementTypeUi
import info.eliumontoyasadec.cryptotracker.ui.screens.movements.SwapDraft
import info.eliumontoyasadec.cryptotracker.ui.screens.movements.SwapFormSheetContent
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

// -------- Domain-ish UI models (still fake) --------

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
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(title, style = MaterialTheme.typography.headlineSmall)

        // Filters
        ElevatedCard {
            Column(
                modifier = Modifier.padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text("Filtros", style = MaterialTheme.typography.titleMedium)
                Divider()

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
                        Text("${state.filteredRows.size}", style = MaterialTheme.typography.labelLarge)
                        OutlinedButton(onClick = onCreate) { Text("Nuevo") }
                    }
                }
                Divider()

                if (state.filteredRows.isEmpty()) {
                    Text(
                        text = "No hay movimientos con los filtros actuales.",
                        style = MaterialTheme.typography.bodyMedium
                    )
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(state.filteredRows, key = { it.id }) { row ->
                            MovementRowItem(
                                row = row,
                                onClick = { onEdit(row) },
                                onEdit = { onEdit(row) },
                                onDelete = { onRequestDelete(row) }
                            )
                            Divider()
                        }
                    }
                }
            }
        }

        // Delete confirmation
        if (state.pendingDeleteId != null) {
            AlertDialog(
                onDismissRequest = onCancelDelete,
                confirmButton = {
                    TextButton(onClick = { onConfirmDelete(state.pendingDeleteId) }) { Text("Eliminar") }
                },
                dismissButton = {
                    TextButton(onClick = onCancelDelete) { Text("Cancelar") }
                },
                title = { Text("Eliminar movimiento") },
                text = { Text("Esta acción no se puede deshacer (fake).") }
            )
        }

        // Bottom sheets
        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

        if (state.movementForm != null) {
            ModalBottomSheet(
                onDismissRequest = onDismissForms,
                sheetState = sheetState
            ) {
                MovementFormSheetContent(
                    mode = state.movementForm.mode,
                    draft = state.movementForm.draft,
                    onChange = onMovementDraftChange,
                    onCancel = onDismissForms,
                    onSave = onMovementSave
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
            .padding(vertical = 6.dp),
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
            Row {
                IconButton(onClick = onEdit) {
                    Icon(Icons.Filled.Edit, contentDescription = "Editar")
                }
                IconButton(onClick = onDelete) {
                    Icon(Icons.Filled.Delete, contentDescription = "Eliminar")
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

// -------- ViewModel (fake data) --------

class MovementsViewModel(private val mode: MovementMode) : ViewModel() {

    private var allRows: List<MovementRow> = fakeRowsFor(mode)

    private val _state = MutableStateFlow(
        MovementsUiState(
            selectedWallet = WalletFilter.ALL,
            selectedCrypto = CryptoFilter.ALL,
            rows = allRows,
            filteredRows = allRows
        )
    )
    val state: StateFlow<MovementsUiState> = _state.asStateFlow()

    fun selectWallet(wallet: WalletFilter) {
        val next = _state.value.copy(selectedWallet = wallet)
        _state.value = next.copy(filteredRows = applyFilters(next))
    }

    fun selectCrypto(crypto: CryptoFilter) {
        val next = _state.value.copy(selectedCrypto = crypto)
        _state.value = next.copy(filteredRows = applyFilters(next))
    }

    fun startCreate() {
        val st = _state.value
        if (mode == MovementMode.SWAP) {
            _state.value = st.copy(
                swapForm = SwapFormState(SwapDraft()),
                movementForm = null
            )
        } else {
            _state.value = st.copy(
                movementForm = MovementFormState(
                    mode = MovementFormMode.CREATE,
                    draft = MovementDraft(type = defaultTypeForMode(mode))
                ),
                swapForm = null
            )
        }
    }

    fun startEdit(row: MovementRow) {
        val st = _state.value
        _state.value = st.copy(
            movementForm = MovementFormState(
                mode = MovementFormMode.EDIT,
                draft = row.toDraft()
            ),
            swapForm = null
        )
    }

    fun dismissForms() {
        val st = _state.value
        _state.value = st.copy(movementForm = null, swapForm = null)
    }

    fun changeMovementDraft(draft: MovementDraft) {
        val st = _state.value
        val form = st.movementForm ?: return
        _state.value = st.copy(movementForm = form.copy(draft = draft))
    }

    fun saveMovement() {
        val st = _state.value
        val form = st.movementForm ?: return
        val draft = form.draft

        val nowId = draft.id ?: "${mode.name.lowercase()}-${System.currentTimeMillis()}"
        val row = draft.toRow(id = nowId, mode = mode)

        allRows = if (form.mode == MovementFormMode.CREATE) {
            listOf(row) + allRows
        } else {
            allRows.map { if (it.id == nowId) row else it }
        }

        val next = st.copy(rows = allRows, movementForm = null)
        _state.value = next.copy(filteredRows = applyFilters(next))
    }

    fun changeSwapDraft(draft: SwapDraft) {
        val st = _state.value
        val form = st.swapForm ?: return
        _state.value = st.copy(swapForm = form.copy(draft = draft))
    }

    fun saveSwap() {
        val st = _state.value
        val form = st.swapForm ?: return
        val draft = form.draft

        val id = draft.id ?: "swap-${System.currentTimeMillis()}"
        val headline = "Swap en ${draft.wallet.label}"
        val details =
            "+ ${draft.toQtyText.ifBlank { "?" }} ${draft.toCrypto.label} · - ${draft.fromQtyText.ifBlank { "?" }} ${draft.fromCrypto.label} (fake)"

        val row = MovementRow(
            id = id,
            dateLabel = draft.dateLabel,
            wallet = draft.wallet,
            crypto = draft.toCrypto,
            headline = headline,
            details = details
        )

        allRows = listOf(row) + allRows
        val next = st.copy(rows = allRows, swapForm = null)
        _state.value = next.copy(filteredRows = applyFilters(next))
    }

    fun requestDelete(row: MovementRow) {
        _state.value = _state.value.copy(pendingDeleteId = row.id)
    }

    fun cancelDelete() {
        _state.value = _state.value.copy(pendingDeleteId = null)
    }

    fun confirmDelete(id: String) {
        allRows = allRows.filterNot { it.id == id }
        val st = _state.value.copy(rows = allRows, pendingDeleteId = null)
        _state.value = st.copy(filteredRows = applyFilters(st))
    }

    private fun applyFilters(state: MovementsUiState): List<MovementRow> {
        return state.rows.filter { row ->
            (state.selectedWallet == WalletFilter.ALL || row.wallet == state.selectedWallet) &&
                    (state.selectedCrypto == CryptoFilter.ALL || row.crypto == state.selectedCrypto)
        }
    }

    private fun defaultTypeForMode(mode: MovementMode): MovementTypeUi {
        return when (mode) {
            MovementMode.IN -> MovementTypeUi.DEPOSIT
            MovementMode.OUT -> MovementTypeUi.WITHDRAW
            MovementMode.BETWEEN -> MovementTypeUi.TRANSFER_OUT
            MovementMode.SWAP -> MovementTypeUi.BUY
        }
    }

    private fun MovementRow.toDraft(): MovementDraft {
        val qty = headline.split(" ").getOrNull(1).orEmpty()
        return MovementDraft(
            id = id,
            wallet = wallet,
            crypto = crypto,
            type = MovementTypeUi.BUY,
            quantityText = qty,
            priceText = "",
            feeQuantityText = "",
            dateLabel = dateLabel,
            notes = details
        )
    }

    private fun MovementDraft.toRow(id: String, mode: MovementMode): MovementRow {
        val qty = quantityText.ifBlank { "?" }
        val head = when (mode) {
            MovementMode.IN -> "+ $qty ${crypto.label}"
            MovementMode.OUT -> "- $qty ${crypto.label}"
            MovementMode.BETWEEN -> "${wallet.label} → (otra)"
            MovementMode.SWAP -> "Movimiento"
        }
        val det = buildString {
            append(type.label)
            append(" · ")
            append("qty=$qty")
            if (priceText.isNotBlank()) append(" · price=$priceText")
            if (feeQuantityText.isNotBlank()) append(" · fee=$feeQuantityText")
            if (notes.isNotBlank()) append(" · ").append(notes)
        }
        return MovementRow(
            id = id,
            dateLabel = dateLabel,
            wallet = wallet,
            crypto = crypto,
            headline = head,
            details = det
        )
    }

    class Factory(private val mode: MovementMode) : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(MovementsViewModel::class.java)) {
                @Suppress("UNCHECKED_CAST")
                return MovementsViewModel(mode) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class")
        }
    }
}

// -------- Fake data --------

private fun fakeRowsFor(mode: MovementMode): List<MovementRow> {
    return when (mode) {
        MovementMode.IN -> listOf(
            MovementRow(
                id = "in-1",
                dateLabel = "15 Feb 2025",
                wallet = WalletFilter.METAMASK,
                crypto = CryptoFilter.BTC,
                headline = "+ 0.10 BTC",
                details = "Entrada a Metamask (fake)"
            ),
            MovementRow(
                id = "in-2",
                dateLabel = "12 Feb 2025",
                wallet = WalletFilter.BYBIT,
                crypto = CryptoFilter.AIXBT,
                headline = "+ 12 AIXBT",
                details = "Entrada a ByBit (fake)"
            )
        )

        MovementMode.OUT -> listOf(
            MovementRow(
                id = "out-1",
                dateLabel = "16 Feb 2025",
                wallet = WalletFilter.METAMASK,
                crypto = CryptoFilter.BTC,
                headline = "- 0.05 BTC",
                details = "Salida desde Metamask (fake)"
            ),
            MovementRow(
                id = "out-2",
                dateLabel = "13 Feb 2025",
                wallet = WalletFilter.PHANTOM,
                crypto = CryptoFilter.SOL,
                headline = "- 1 SOL",
                details = "Salida desde Phantom (fake)"
            )
        )

        MovementMode.BETWEEN -> listOf(
            MovementRow(
                id = "btw-1",
                dateLabel = "14 Feb 2025",
                wallet = WalletFilter.METAMASK,
                crypto = CryptoFilter.ALGO,
                headline = "Metamask → Phantom",
                details = "3 ALGO transferidos (fake)"
            ),
            MovementRow(
                id = "btw-2",
                dateLabel = "12 Feb 2025",
                wallet = WalletFilter.BYBIT,
                crypto = CryptoFilter.AIXBT,
                headline = "ByBit → Metamask",
                details = "2 AIXBT transferidos (fake)"
            )
        )

        MovementMode.SWAP -> listOf(
            MovementRow(
                id = "sw-1",
                dateLabel = "13 Feb 2025",
                wallet = WalletFilter.METAMASK,
                crypto = CryptoFilter.AIXBT,
                headline = "Swap en Metamask",
                details = "+ 1 AIXBT · - 5 ALGO (fake)"
            ),
            MovementRow(
                id = "sw-2",
                dateLabel = "11 Feb 2025",
                wallet = WalletFilter.PHANTOM,
                crypto = CryptoFilter.SOL,
                headline = "Swap en Phantom",
                details = "+ 2 SOL · - 0.01 BTC (fake)"
            )
        )
    }
}