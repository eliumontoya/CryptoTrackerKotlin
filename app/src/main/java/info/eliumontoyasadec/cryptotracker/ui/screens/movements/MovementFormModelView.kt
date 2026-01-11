package info.eliumontoyasadec.cryptotracker.ui.screens.movements

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

data class MovementFormUiState(
    val mode: MovementFormMode,
    val draft: MovementDraft,

    // Validación derivada (para habilitar Guardar de forma estable)
    val quantityError: String? = null,
    val priceError: String? = null,
    val feeError: String? = null,
    val canSave: Boolean = false,

    // DatePicker: estado explícito (evita flakes por remember local)
    val showDatePicker: Boolean = false,
    val selectedDateMillis: Long? = null
)

class MovementFormModelView(
    initialMode: MovementFormMode,
    initialDraft: MovementDraft,
    private val onCancelExternal: () -> Unit,
    private val onDraftChangeExternal: (MovementDraft) -> Unit,
    private val onSaveExternal: () -> Unit
) {

    var state: MovementFormUiState =
        MovementFormUiState(mode = initialMode, draft = initialDraft).revalidated()

    fun onCancel() = onCancelExternal()

    fun onSave() {
        if (!state.canSave) return
        onSaveExternal()
    }

    fun onDraftChange(draft: MovementDraft) {
        // Fuente de verdad local para la UI
        state = state.copy(draft = draft).revalidated()
        // …pero también sincronizamos con tu VM actual (para que save/edit funcione igual)
        onDraftChangeExternal(draft)
    }

    fun onWalletSelect(walletId: Long?) {
        onDraftChange(state.draft.copy(walletId = walletId))
    }

    fun onCryptoSelect(crypto: CryptoFilter) {
        onDraftChange(state.draft.copy(crypto = crypto))
    }

    fun onTypeSelect(type: MovementTypeUi) {
        onDraftChange(state.draft.copy(type = type))
    }

    fun onQuantityChange(v: String) {
        onDraftChange(state.draft.copy(quantityText = v))
    }

    fun onPriceChange(v: String) {
        onDraftChange(state.draft.copy(priceText = v))
    }

    fun onFeeChange(v: String) {
        onDraftChange(state.draft.copy(feeQuantityText = v))
    }

    fun onNotesChange(v: String) {
        onDraftChange(state.draft.copy(notes = v))
    }

    fun openDatePicker() {
        state = state.copy(showDatePicker = true)
    }

    fun dismissDatePicker() {
        state = state.copy(showDatePicker = false)
    }

    fun onDatePicked(millis: Long?) {
        if (millis == null) {
            state = state.copy(showDatePicker = false)
            return
        }
        val label = formatMillisToLabel(millis)
        onDraftChange(state.draft.copy(dateLabel = label))
        state = state.copy(showDatePicker = false, selectedDateMillis = millis).revalidated()
    }

    private fun MovementFormUiState.revalidated(): MovementFormUiState {
        val qtyRaw = draft.quantityText.trim()
        val priceRaw = draft.priceText.trim()
        val feeRaw = draft.feeQuantityText.trim()

        val qtyValue = qtyRaw.toDoubleOrNull()
        val priceValue = if (priceRaw.isBlank()) 0.0 else priceRaw.toDoubleOrNull()
        val feeValue = if (feeRaw.isBlank()) 0.0 else feeRaw.toDoubleOrNull()

        val qtyOk = (qtyValue != null && qtyValue > 0.0)
        val priceOk = (priceRaw.isBlank() || (priceValue != null && priceValue >= 0.0))
        val feeOk = (feeRaw.isBlank() || (feeValue != null && feeValue >= 0.0))

        return copy(
            quantityError = if (qtyOk) null else if (qtyRaw.isBlank()) "Requerido" else "Cantidad inválida",
            priceError = if (priceOk) null else "Precio inválido",
            feeError = if (feeOk) null else "Fee inválido",
            canSave = qtyOk && priceOk && feeOk
        )
    }

    private fun formatMillisToLabel(millis: Long): String {
        val formatter = DateTimeFormatter.ofPattern("dd MMM yyyy", Locale.getDefault())
        val localDate: LocalDate =
            Instant.ofEpochMilli(millis).atZone(ZoneId.systemDefault()).toLocalDate()
        return formatter.format(localDate)
    }
}