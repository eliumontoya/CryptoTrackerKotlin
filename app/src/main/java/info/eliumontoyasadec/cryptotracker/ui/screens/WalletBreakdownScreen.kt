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
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.remember
import info.eliumontoyasadec.cryptotracker.ui.screens.movements.MovementDraft
import info.eliumontoyasadec.cryptotracker.ui.screens.movements.MovementFormMode
import info.eliumontoyasadec.cryptotracker.ui.screens.movements.MovementFormModelView
import info.eliumontoyasadec.cryptotracker.ui.screens.movements.MovementFormSheetContent

// -------- UI models --------

data class WalletBreakdownRow(
    val walletName: String,
    val valueUsd: Double,
    val pnlUsd: Double
)

enum class WalletSortBy(val label: String) {
    VALUE("Valor"),
    PNL("P&L")
}

// -------- UI State --------

data class WalletBreakdownUiState(
    val showEmpty: Boolean = false,
    val sortBy: WalletSortBy = WalletSortBy.VALUE,
    val rows: List<WalletBreakdownRow> = emptyList(),
    val visibleRows: List<WalletBreakdownRow> = emptyList(),
    val movementForm: WalletBreakdownMovementFormState? = null)

data class WalletBreakdownMovementFormState(
    val mode: MovementFormMode,
    val draft: MovementDraft
)

// -------- Composable --------

@Composable
fun WalletBreakdownScreen(
    state: WalletBreakdownUiState,
    onToggleShowEmpty: () -> Unit,
    onChangeSort: (WalletSortBy) -> Unit,
    onWalletClick: (walletName: String) -> Unit,
    onAddMovement: () -> Unit,
    onDismissForm: () -> Unit,
    onMovementDraftChange: (MovementDraft) -> Unit,
    onMovementSave: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text("Desglose por Carteras", style = MaterialTheme.typography.headlineSmall)
            IconButton(onClick = onAddMovement) {
                Icon(Icons.Filled.Add, contentDescription = "Agregar movimiento")
            }
        }

        // Filters
        ElevatedCard {
            Column(
                modifier = Modifier.padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text("Filtros", style = MaterialTheme.typography.titleMedium)
                HorizontalDivider()

                Text("Mostrar", style = MaterialTheme.typography.labelLarge)
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    item {
                        FilterChip(
                            selected = state.showEmpty,
                            onClick = onToggleShowEmpty,
                            label = { Text("Incluir vacías") }
                        )
                    }
                }

                Spacer(Modifier.height(6.dp))

                Text("Orden", style = MaterialTheme.typography.labelLarge)
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(WalletSortBy.entries) { opt ->
                        FilterChip(
                            selected = opt == state.sortBy,
                            onClick = { onChangeSort(opt) },
                            label = { Text(opt.label) }
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
                    Text("Carteras", style = MaterialTheme.typography.titleMedium)
                    Text("${state.visibleRows.size}", style = MaterialTheme.typography.labelLarge)
                }
                HorizontalDivider()

                if (state.visibleRows.isEmpty()) {
                    Text(
                        text = "No hay carteras para mostrar con los filtros actuales.",
                        style = MaterialTheme.typography.bodyMedium
                    )
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(state.visibleRows, key = { it.walletName }) { row ->
                            WalletRowItem(row = row, onClick = { onWalletClick(row.walletName) })
                            HorizontalDivider()
                        }
                    }
                }
            }
        }

        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

        if (state.movementForm != null) {
            ModalBottomSheet(
                onDismissRequest = onDismissForm,
                sheetState = sheetState
            ) {
                val form = state.movementForm

                 val formMv = remember(form.mode, form.draft.id) {
                    MovementFormModelView(
                        initialMode = form.mode,
                        initialDraft = form.draft,
                        onCancelExternal = onDismissForm,
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
    }
}

@Composable
private fun WalletRowItem(
    row: WalletBreakdownRow,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 10.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(Modifier.weight(1f)) {
            Text(row.walletName, style = MaterialTheme.typography.titleMedium)
            Text(
                text = "Valor: ${formatUsd(row.valueUsd)}",
                style = MaterialTheme.typography.bodySmall
            )
        }
        Text(formatUsd(row.pnlUsd), style = MaterialTheme.typography.titleMedium)
    }
}

private fun formatUsd(v: Double): String = "$" + "%,.2f".format(v)

// -------- ViewModel (fake data) --------

class WalletBreakdownViewModel : ViewModel() {

    private val allRows = fakeWalletRows()

    private val _state = MutableStateFlow(
        WalletBreakdownUiState(
            rows = allRows,
            visibleRows = allRows
        )
    )
    val state: StateFlow<WalletBreakdownUiState> = _state.asStateFlow()

    fun toggleShowEmpty() {
        val next = _state.value.copy(showEmpty = !_state.value.showEmpty)
        _state.value = next.copy(visibleRows = apply(next))
    }

    fun changeSort(sort: WalletSortBy) {
        val next = _state.value.copy(sortBy = sort)
        _state.value = next.copy(visibleRows = apply(next))
    }

    fun startAddMovement() {
        val st = _state.value
        _state.value = st.copy(
            movementForm = WalletBreakdownMovementFormState(
                mode = MovementFormMode.CREATE,
                draft = MovementDraft(wallet = WalletFilter.METAMASK)
            )
        )
    }

    fun dismissForm() {
        _state.value = _state.value.copy(movementForm = null)
    }

    fun changeMovementDraft(draft: MovementDraft) {
        val st = _state.value
        val form = st.movementForm ?: return
        _state.value = st.copy(movementForm = form.copy(draft = draft))
    }

    fun saveMovement() {
        // UI-only: close the sheet. (Later: call use case / repo)
        dismissForm()
    }

    private fun apply(state: WalletBreakdownUiState): List<WalletBreakdownRow> {
        val filtered = state.rows.filter {
            state.showEmpty || it.valueUsd > 0.0 || it.pnlUsd != 0.0
        }

        return when (state.sortBy) {
            WalletSortBy.VALUE -> filtered.sortedByDescending { it.valueUsd }
            WalletSortBy.PNL -> filtered.sortedByDescending { it.pnlUsd }
        }
    }
}

private fun fakeWalletRows(): List<WalletBreakdownRow> = listOf(
    WalletBreakdownRow("Metamask", 10_400.0, 650.0),
    WalletBreakdownRow("ByBit", 2_100.0, 190.0),
    WalletBreakdownRow("Phantom", 0.0, 0.0)
)