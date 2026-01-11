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
import info.eliumontoyasadec.cryptotracker.domain.model.Wallet
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
data class SwapDraft(
    val id: Long? = null,
    val walletId: Long? = null,              // ✅ Opción A: id real
    val fromCrypto: CryptoFilter = CryptoFilter.BTC,
    val toCrypto: CryptoFilter = CryptoFilter.AIXBT,
    val fromQtyText: String = "",
    val toQtyText: String = "",
    val feeQtyText: String = "",
    val dateLabel: String = "Hoy",
    val notes: String = ""
)

@Composable
fun SwapFormSheetContent(
    draft: SwapDraft,
    wallets: List<Wallet>,
    onDraftChange: (SwapDraft) -> Unit,
    onCancel: () -> Unit,
    onSave: () -> Unit
) {
    val fromQtyRaw = draft.fromQtyText.trim()
    val toQtyRaw = draft.toQtyText.trim()

    val canSave =
        (draft.walletId != null) &&
                fromQtyRaw.toDoubleOrNull() != null &&
                toQtyRaw.toDoubleOrNull() != null &&
                draft.fromCrypto != CryptoFilter.ALL &&
                draft.toCrypto != CryptoFilter.ALL &&
                draft.fromCrypto != draft.toCrypto

    var showDatePicker by remember { mutableStateOf(false) }
    val datePickerState = rememberDatePickerState()

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
            .testTag(MovementTags.SwapSheet),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("Nuevo swap", style = MaterialTheme.typography.titleLarge)

        // Si todavía estás en modo "fake", ajusta el texto como gustes.
        Text("Selecciona una cartera real (BD) y captura cantidades.", style = MaterialTheme.typography.bodySmall)

        HorizontalDivider()

        Text("Cartera", style = MaterialTheme.typography.labelLarge)

        if (wallets.isEmpty()) {
            Text(
                "No hay wallets disponibles. Crea una wallet primero.",
                style = MaterialTheme.typography.bodyMedium
            )
        } else {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(wallets, key = { it.walletId }) { w ->
                    FilterChip(
                        selected = draft.walletId == w.walletId,
                        onClick = { onDraftChange(draft.copy(walletId = w.walletId)) },
                        label = { Text(w.name) }
                    )
                }
            }
        }

        HorizontalDivider()

        Text("Activos", style = MaterialTheme.typography.labelLarge)

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Column(Modifier.weight(1f)) {
                Text("Vendes", style = MaterialTheme.typography.labelMedium)
                ChipRow(
                    options = CryptoFilter.entries.filter { it != CryptoFilter.ALL },
                    selected = draft.fromCrypto,
                    labelOf = { it.label },
                    onSelect = { onDraftChange(draft.copy(fromCrypto = it)) }
                )
            }
            Column(Modifier.weight(1f)) {
                Text("Compras", style = MaterialTheme.typography.labelMedium)
                ChipRow(
                    options = CryptoFilter.entries.filter { it != CryptoFilter.ALL },
                    selected = draft.toCrypto,
                    labelOf = { it.label },
                    onSelect = { onDraftChange(draft.copy(toCrypto = it)) }
                )
            }
        }

        OutlinedTextField(
            value = draft.fromQtyText,
            onValueChange = { onDraftChange(draft.copy(fromQtyText = it)) },
            label = { Text("Cantidad vendida") },
            modifier = Modifier.fillMaxWidth(),
            keyboardOptions =  KeyboardOptions(keyboardType = KeyboardType.Number)
        )

        OutlinedTextField(
            value = draft.toQtyText,
            onValueChange = { onDraftChange(draft.copy(toQtyText = it)) },
            label = { Text("Cantidad comprada") },
            modifier = Modifier.fillMaxWidth(),
            keyboardOptions =  KeyboardOptions(keyboardType = KeyboardType.Number)
        )

        OutlinedTextField(
            value = draft.feeQtyText,
            onValueChange = { onDraftChange(draft.copy(feeQtyText = it)) },
            label = { Text("Fee (opcional)") },
            modifier = Modifier.fillMaxWidth(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)        )

        HorizontalDivider()

        Text("Fecha", style = MaterialTheme.typography.labelLarge)

        OutlinedButton(
            onClick = { showDatePicker = true },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(draft.dateLabel)
        }

        OutlinedTextField(
            value = draft.notes,
            onValueChange = { onDraftChange(draft.copy(notes = it)) },
            label = { Text("Notas (opcional)") },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(Modifier.height(8.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            OutlinedButton(
                onClick = onCancel,
                modifier = Modifier.weight(1f)
            ) { Text("Cancelar") }

            Button(
                onClick = onSave,
                enabled = canSave,
                modifier = Modifier
                    .weight(1f)
                    .testTag(MovementTags.SwapFormSave)
            ) { Text("Guardar") }
        }
    }

    if (showDatePicker) {
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        // Mantengo simple: solo marca "Elegida" sin convertir millis -> fecha,
                        // para no inventar formato si ya tienes utilidades en otro archivo.
                        // Si quieres, lo refinamos con tu formatter existente.
                        onDraftChange(draft.copy(dateLabel = "Elegida"))
                        showDatePicker = false
                    }
                ) { Text("OK") }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) { Text("Cancelar") }
            }
        ) {
            DatePicker(state = datePickerState)
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