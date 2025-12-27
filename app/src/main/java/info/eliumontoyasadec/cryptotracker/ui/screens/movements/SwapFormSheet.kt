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
import androidx.compose.material3.Divider
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import info.eliumontoyasadec.cryptotracker.ui.screens.CryptoFilter
import info.eliumontoyasadec.cryptotracker.ui.screens.WalletFilter

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
            singleLine = true
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
            singleLine = true
        )

        OutlinedTextField(
            value = draft.feeQtyText,
            onValueChange = { onChange(draft.copy(feeQtyText = it)) },
            label = { Text("Fee (qty) - opcional") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )

        OutlinedTextField(
            value = draft.notes,
            onValueChange = { onChange(draft.copy(notes = it)) },
            label = { Text("Notas") },
            modifier = Modifier.fillMaxWidth()
        )

        Text("Fecha: ${draft.dateLabel} (placeholder)", style = MaterialTheme.typography.bodySmall)

        Spacer(Modifier.height(4.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End
        ) {
            TextButton(onClick = onCancel) { Text("Cancelar") }
            Button(onClick = onSave) { Text("Guardar swap") }
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