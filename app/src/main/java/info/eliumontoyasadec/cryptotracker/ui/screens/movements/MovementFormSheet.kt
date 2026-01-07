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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import info.eliumontoyasadec.cryptotracker.ui.screens.CryptoFilter
import info.eliumontoyasadec.cryptotracker.ui.screens.WalletFilter
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

enum class MovementFormMode { CREATE, EDIT }

// Draft “UI-only” (después lo mapearás a tu MovementRow / Movement domain)
data class MovementDraft(
    val id: String? = null,
    val wallet: WalletFilter = WalletFilter.METAMASK,
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
    mode: MovementFormMode,
    draft: MovementDraft,
    onChange: (MovementDraft) -> Unit,
    onCancel: () -> Unit,
    onSave: () -> Unit
) {
    val qtyRaw = draft.quantityText.trim()
    val priceRaw = draft.priceText.trim()
    val feeRaw = draft.feeQuantityText.trim()

    val qtyValue = qtyRaw.toDoubleOrNull()
    val priceValue = if (priceRaw.isBlank()) 0.0 else priceRaw.toDoubleOrNull()
    val feeValue = if (feeRaw.isBlank()) 0.0 else feeRaw.toDoubleOrNull()

    val qtyOk = (qtyValue != null && qtyValue > 0.0)
    val priceOk = (priceRaw.isBlank() || (priceValue != null && priceValue >= 0.0))
    val feeOk = (feeRaw.isBlank() || (feeValue != null && feeValue >= 0.0))

    val canSave = qtyOk && priceOk && feeOk

    var showDatePicker by remember { mutableStateOf(false) }
    val datePickerState = rememberDatePickerState()

    val formatter = remember {
        DateTimeFormatter.ofPattern("dd MMM yyyy", Locale.getDefault())
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 10.dp)
            .testTag(MovementTags.FormSheet),

        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = if (mode == MovementFormMode.CREATE) "Nuevo movimiento" else "Editar movimiento",
            style = MaterialTheme.typography.titleLarge
        )
        Text(
            "(sin wiring) Este formulario actualiza estado fake.",
            style = MaterialTheme.typography.bodySmall
        )

        HorizontalDivider()

        Text("Cartera", style = MaterialTheme.typography.labelLarge)
        ChipRow(
            options = WalletFilter.entries.filter { it != WalletFilter.ALL },
            selected = draft.wallet,
            labelOf = { it.label },
            onSelect = { onChange(draft.copy(wallet = it)) }
        )

        Text("Crypto", style = MaterialTheme.typography.labelLarge)
        ChipRow(
            options = CryptoFilter.entries.filter { it != CryptoFilter.ALL },
            selected = draft.crypto,
            labelOf = { it.label },
            onSelect = { onChange(draft.copy(crypto = it)) }
        )

        Text("Tipo", style = MaterialTheme.typography.labelLarge)
        ChipRow(
            options = MovementTypeUi.entries,
            selected = draft.type,
            labelOf = { it.label },
            onSelect = { onChange(draft.copy(type = it)) },
            modifier = Modifier.testTag(MovementTags.FormTypeChips),
            chipTagOf = { MovementTags.formTypeChip(it.name) }
        )

        OutlinedTextField(
            value = draft.quantityText,
            onValueChange = { onChange(draft.copy(quantityText = it)) },
            label = { Text("Cantidad") },
            modifier = Modifier
                .fillMaxWidth()
                .testTag(MovementTags.FormQuantity),
            singleLine = true,
            isError = !qtyOk,
            supportingText = {
                if (!qtyOk) {
                    Text(if (qtyRaw.isBlank()) "Requerido" else "Cantidad inválida")
                }
            }
        )

        OutlinedTextField(
            value = draft.priceText,
            onValueChange = { onChange(draft.copy(priceText = it)) },
            label = { Text("Precio (USD) - opcional") },
            modifier = Modifier
                .fillMaxWidth()
                .testTag(MovementTags.FormPrice),
            singleLine = true,
            isError = !priceOk,
            supportingText = {
                if (!priceOk) Text("Precio inválido")
            }
        )

        OutlinedTextField(
            value = draft.feeQuantityText,
            onValueChange = { onChange(draft.copy(feeQuantityText = it)) },
            label = { Text("Fee (qty) - opcional") },
            modifier = Modifier
                .fillMaxWidth()
                .testTag(MovementTags.FormFee),
            singleLine = true,
            isError = !feeOk,
            supportingText = {
                if (!feeOk) Text("Fee inválido")
            }
        )

        OutlinedTextField(
            value = draft.notes,
            onValueChange = { onChange(draft.copy(notes = it)) },
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
                onClick = { showDatePicker = true },
                modifier = Modifier.testTag(MovementTags.FormPickDate)
            ) {
                Text("Elegir")
            }
        }

        if (showDatePicker) {
            DatePickerDialog(
                onDismissRequest = { showDatePicker = false },
                confirmButton = {
                    TextButton(
                        onClick = {
                            val millis = datePickerState.selectedDateMillis
                            if (millis != null) {
                                val localDate = Instant.ofEpochMilli(millis)
                                    .atZone(ZoneId.systemDefault())
                                    .toLocalDate()
                                val label = formatter.format(localDate)
                                onChange(draft.copy(dateLabel = label))
                            }
                            showDatePicker = false
                        }
                    ) { Text("Aceptar") }
                },
                dismissButton = {
                    TextButton(onClick = { showDatePicker = false }
                    ) { Text("Cancelar") }
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
                onClick = onCancel,
                modifier = Modifier.testTag(MovementTags.FormCancel)
            ) { Text("Cancelar") }
            Button(
                onClick = onSave,
                enabled = canSave,
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