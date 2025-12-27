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
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = if (mode == MovementFormMode.CREATE) "Nuevo movimiento" else "Editar movimiento",
            style = MaterialTheme.typography.titleLarge
        )
        Text("(sin wiring) Este formulario actualiza estado fake.", style = MaterialTheme.typography.bodySmall)

        Divider()

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
            onSelect = { onChange(draft.copy(type = it)) }
        )

        OutlinedTextField(
            value = draft.quantityText,
            onValueChange = { onChange(draft.copy(quantityText = it)) },
            label = { Text("Cantidad") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )

        OutlinedTextField(
            value = draft.priceText,
            onValueChange = { onChange(draft.copy(priceText = it)) },
            label = { Text("Precio (USD) - opcional") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )

        OutlinedTextField(
            value = draft.feeQuantityText,
            onValueChange = { onChange(draft.copy(feeQuantityText = it)) },
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
            Button(onClick = onSave) { Text("Guardar") }
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