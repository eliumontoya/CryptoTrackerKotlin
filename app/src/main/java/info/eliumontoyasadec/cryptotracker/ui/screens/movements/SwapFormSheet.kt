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
import androidx.compose.material3.Divider
import androidx.compose.material3.FilterChip
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
import androidx.compose.ui.unit.dp
import info.eliumontoyasadec.cryptotracker.ui.screens.CryptoFilter
import info.eliumontoyasadec.cryptotracker.ui.screens.WalletFilter
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

data class SwapDraft(
    val id: String? = null,
    val wallet: WalletFilter = WalletFilter.METAMASK,
    val fromCrypto: CryptoFilter = CryptoFilter.ALGO,
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
    onChange: (SwapDraft) -> Unit,
    onCancel: () -> Unit,
    onSave: () -> Unit
) {
    val fromQtyRaw = draft.fromQtyText.trim()
    val toQtyRaw = draft.toQtyText.trim()
    val feeRaw = draft.feeQtyText.trim()

    val fromQtyValue = fromQtyRaw.toDoubleOrNull()
    val toQtyValue = toQtyRaw.toDoubleOrNull()
    val feeValue = if (feeRaw.isBlank()) 0.0 else feeRaw.toDoubleOrNull()

    val fromOk = (fromQtyValue != null && fromQtyValue > 0.0)
    val toOk = (toQtyValue != null && toQtyValue > 0.0)
    val feeOk = (feeRaw.isBlank() || (feeValue != null && feeValue >= 0.0))
    val pairOk = (draft.fromCrypto != draft.toCrypto)

    val canSave = fromOk && toOk && feeOk && pairOk

    var showDatePicker by remember { mutableStateOf(false) }
    val datePickerState = rememberDatePickerState()
    val formatter = remember {
        DateTimeFormatter.ofPattern("dd MMM yyyy", Locale.getDefault())
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("Nuevo swap", style = MaterialTheme.typography.titleLarge)
        Text("(sin wiring) Este swap generará movimientos fake.", style = MaterialTheme.typography.bodySmall)

        Divider()

        Text("Cartera", style = MaterialTheme.typography.labelLarge)
        ChipRow(
            options = WalletFilter.entries.filter { it != WalletFilter.ALL },
            selected = draft.wallet,
            labelOf = { it.label },
            onSelect = { onChange(draft.copy(wallet = it)) }
        )

        Text("From", style = MaterialTheme.typography.labelLarge)
        ChipRow(
            options = CryptoFilter.entries.filter { it != CryptoFilter.ALL },
            selected = draft.fromCrypto,
            labelOf = { it.label },
            onSelect = { onChange(draft.copy(fromCrypto = it)) }
        )
        OutlinedTextField(
            value = draft.fromQtyText,
            onValueChange = { onChange(draft.copy(fromQtyText = it)) },
            label = { Text("Cantidad From") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            isError = !fromOk,
            supportingText = {
                if (!fromOk) {
                    Text(if (fromQtyRaw.isBlank()) "Requerido" else "Cantidad inválida")
                }
            }
        )

        Text("To", style = MaterialTheme.typography.labelLarge)
        ChipRow(
            options = CryptoFilter.entries.filter { it != CryptoFilter.ALL },
            selected = draft.toCrypto,
            labelOf = { it.label },
            onSelect = { onChange(draft.copy(toCrypto = it)) }
        )
        OutlinedTextField(
            value = draft.toQtyText,
            onValueChange = { onChange(draft.copy(toQtyText = it)) },
            label = { Text("Cantidad To") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            isError = !toOk || !pairOk,
            supportingText = {
                when {
                    !pairOk -> Text("From y To deben ser diferentes")
                    !toOk -> Text(if (toQtyRaw.isBlank()) "Requerido" else "Cantidad inválida")
                }
            }
        )

        OutlinedTextField(
            value = draft.feeQtyText,
            onValueChange = { onChange(draft.copy(feeQtyText = it)) },
            label = { Text("Fee (qty) - opcional") },
            modifier = Modifier.fillMaxWidth(),
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
            modifier = Modifier.fillMaxWidth()
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text("Fecha: ${draft.dateLabel}", style = MaterialTheme.typography.bodySmall)
            OutlinedButton(onClick = { showDatePicker = true }) { Text("Elegir") }
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
                    TextButton(onClick = { showDatePicker = false }) { Text("Cancelar") }
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
            TextButton(onClick = onCancel) { Text("Cancelar") }
            Button(onClick = onSave, enabled = canSave) { Text("Guardar swap") }
        }

        Spacer(Modifier.height(12.dp))
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