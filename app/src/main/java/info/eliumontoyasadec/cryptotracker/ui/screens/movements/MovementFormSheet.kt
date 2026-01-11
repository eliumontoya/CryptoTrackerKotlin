@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package info.eliumontoyasadec.cryptotracker.ui.screens.movements

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import info.eliumontoyasadec.cryptotracker.domain.model.Wallet

enum class MovementFormMode { CREATE, EDIT }

// Draft “UI-only” (después lo mapearás a tu MovementRow / Movement domain)
data class MovementDraft(
    val id: String? = null,
    val walletId: Long? = null,
    val crypto: CryptoFilter = CryptoFilter.BTC,
    val type: MovementTypeUi = MovementTypeUi.BUY,
    val quantityText: String = "",
    val priceText: String = "",
    val feeQuantityText: String = "",
    val dateLabel: String = "Hoy", // placeholder
    val notes: String = ""
)

enum class MovementTypeUi(val label: String) {
    BUY("BUY"),
    SELL("SELL"),
    DEPOSIT("DEPOSIT"),
    WITHDRAW("WITHDRAW"),
    TRANSFER_IN("TRANSFER_IN"),
    TRANSFER_OUT("TRANSFER_OUT"),
    FEE("FEE")
}

@Composable
fun MovementFormSheetContent(
    state: MovementFormUiState,
    mv: MovementFormModelView,
    wallets: List<Wallet>
) {
    val draft = state.draft

    val datePickerState = rememberDatePickerState(
        initialSelectedDateMillis = state.selectedDateMillis
    )

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 10.dp)
            .testTag(MovementTags.FormSheet),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = if (state.mode == MovementFormMode.CREATE) "Nuevo movimiento" else "Editar movimiento",
            style = MaterialTheme.typography.titleLarge
        )
        Text(
            "(sin wiring) Este formulario actualiza estado fake.",
            style = MaterialTheme.typography.bodySmall
        )

        HorizontalDivider()

        Text("Cartera", style = MaterialTheme.typography.labelLarge)
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(wallets, key = { it.walletId }) { w ->
                FilterChip(
                    selected = draft.walletId == w.walletId,
                    onClick = { mv.onWalletSelect(w.walletId) },
                    label = { Text(w.name) }
                )
            }
        }

        Text("Crypto", style = MaterialTheme.typography.labelLarge)
        ChipRow(
            options = CryptoFilter.entries.filter { it != CryptoFilter.ALL },
            selected = draft.crypto,
            labelOf = { it.label },
            onSelect = { mv.onCryptoSelect(it) }
        )

        Text("Tipo", style = MaterialTheme.typography.labelLarge)
        ChipRow(
            options = MovementTypeUi.entries,
            selected = draft.type,
            labelOf = { it.label },
            onSelect = { mv.onTypeSelect(it) },
            modifier = Modifier.testTag(MovementTags.FormTypeChips),
            chipTagOf = { MovementTags.formTypeChip(it.name) }
        )

        OutlinedTextField(
            value = draft.quantityText,
            onValueChange = { mv.onQuantityChange(it) },
            label = { Text("Cantidad") },
            modifier = Modifier
                .fillMaxWidth()
                .testTag(MovementTags.FormQuantity),
            singleLine = true,
            isError = state.quantityError != null,
            supportingText = { state.quantityError?.let { Text(it) } }
        )

        OutlinedTextField(
            value = draft.priceText,
            onValueChange = { mv.onPriceChange(it) },
            label = { Text("Precio (USD) - opcional") },
            modifier = Modifier
                .fillMaxWidth()
                .testTag(MovementTags.FormPrice),
            singleLine = true,
            isError = state.priceError != null,
            supportingText = { state.priceError?.let { Text(it) } }
        )

        OutlinedTextField(
            value = draft.feeQuantityText,
            onValueChange = { mv.onFeeChange(it) },
            label = { Text("Fee (qty) - opcional") },
            modifier = Modifier
                .fillMaxWidth()
                .testTag(MovementTags.FormFee),
            singleLine = true,
            isError = state.feeError != null,
            supportingText = { state.feeError?.let { Text(it) } }
        )

        OutlinedTextField(
            value = draft.notes,
            onValueChange = { mv.onNotesChange(it) },
            label = { Text("Notas") },
            modifier = Modifier
                .fillMaxWidth()
                .testTag(MovementTags.FormNotes)
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "Fecha: ${draft.dateLabel}",
                style = MaterialTheme.typography.bodySmall
            )
            OutlinedButton(
                onClick = { mv.openDatePicker() },
                modifier = Modifier.testTag(MovementTags.FormPickDate)
            ) {
                Text("Elegir")
            }
        }

        if (state.showDatePicker) {
            DatePickerDialog(
                onDismissRequest = { mv.dismissDatePicker() },
                confirmButton = {
                    TextButton(
                        onClick = { mv.onDatePicked(datePickerState.selectedDateMillis) }
                    ) { Text("Aceptar") }
                },
                dismissButton = {
                    TextButton(onClick = { mv.dismissDatePicker() }) { Text("Cancelar") }
                }
            ) {
                DatePicker(state = datePickerState)
            }
        }

        Spacer(Modifier.height(4.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End
        ) {
            TextButton(
                onClick = { mv.onCancel() },
                modifier = Modifier.testTag(MovementTags.FormCancel)
            ) { Text("Cancelar") }

            Button(
                onClick = { mv.onSave() },
                enabled = state.canSave,
                modifier = Modifier.testTag(MovementTags.FormSave)
            ) { Text("Guardar") }
        }

        Spacer(Modifier.height(12.dp))
    }
}

@Composable
private fun <T> ChipRow(
    options: List<T>,
    selected: T,
    labelOf: (T) -> String,
    onSelect: (T) -> Unit,
    modifier: Modifier = Modifier,
    chipTagOf: ((T) -> String)? = null
) {
    LazyRow(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(options) { opt ->
            val chipModifier =
                chipTagOf?.let { Modifier.testTag(it(opt)) } ?: Modifier

            FilterChip(
                modifier = chipModifier,
                selected = opt == selected,
                onClick = { onSelect(opt) },
                label = { Text(labelOf(opt)) }
            )
        }
    }
}