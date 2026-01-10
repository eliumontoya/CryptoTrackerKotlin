package info.eliumontoyasadec.cryptotracker.ui.screens.movements

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import info.eliumontoyasadec.cryptotracker.domain.interactor.movement.DeleteMovementCommand
import info.eliumontoyasadec.cryptotracker.domain.interactor.movement.DeleteMovementUseCase
import info.eliumontoyasadec.cryptotracker.domain.interactor.movement.EditMovementCommand
import info.eliumontoyasadec.cryptotracker.domain.interactor.movement.EditMovementUseCase
import info.eliumontoyasadec.cryptotracker.domain.interactor.movement.MoveBetweenWalletsUseCase
import info.eliumontoyasadec.cryptotracker.domain.interactor.movement.RegisterMovementCommand
import info.eliumontoyasadec.cryptotracker.domain.interactor.movement.RegisterMovementUseCase
import info.eliumontoyasadec.cryptotracker.domain.interactor.movement.SwapMovementCommand
import info.eliumontoyasadec.cryptotracker.domain.interactor.movement.SwapMovementUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class MovementsViewModel(private val mode: MovementMode,

    // UseCases (inyectados).
                         private val registerMovement: RegisterMovementUseCase? = null,
                         private val editMovement: EditMovementUseCase? = null,
                         private val deleteMovement: DeleteMovementUseCase? = null,
                         private val swapMovement: SwapMovementUseCase? = null,

    //Todo: Aún no lo invocamos: falta UI suficiente para construir el command sin cambiar UX.
                         private val moveBetweenWallets: MoveBetweenWalletsUseCase? = null,

    // Todo: Resolvers mínimos para poder armar Commands sin que UI conozca data/room
                         private val portfolioIdProvider: () -> Long = { 1L },
                         private val walletIdResolver: (WalletFilter) -> Long? = ::defaultMovementWalletId,
                         private val assetIdResolver: (CryptoFilter) -> String? = ::defaultMovementAssetId
    ) : ViewModel() {

    private var allRows: List<MovementRow> = fakeRowsFor(mode)
    // Solo para filas creadas productivamente (cuando podamos re-etiquetar ids fake → id real)
    private val idMap: MutableMap<String, Long> = mutableMapOf()

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

        // ---- UI first (mantener E2E): actualiza lista local tal cual hoy ----
        val fallbackId = draft.id ?: "${mode.name.lowercase()}-${System.currentTimeMillis()}"
        val optimisticRow = draft.toRow(id = fallbackId, mode = mode)

        allRows = if (form.mode == MovementFormMode.CREATE) {
            listOf(optimisticRow) + allRows
        } else {
            allRows.map { if (it.id == fallbackId) optimisticRow else it }
        }

        val next = st.copy(rows = allRows, movementForm = null)
        _state.value = next.copy(filteredRows = applyFilters(next))

        // ---- Side-effect productivo (si hay UseCases y podemos construir command) ----
        when (form.mode) {
            MovementFormMode.CREATE -> {
                val uc = registerMovement ?: return
                viewModelScope.launch {
                    runCatching {
                        val portfolioId = portfolioIdProvider()
                        val walletId = walletIdResolver(draft.wallet) ?: return@runCatching
                        val assetId = assetIdResolver(draft.crypto) ?: return@runCatching

                        val qty = draft.quantityText.toDoubleOrNull() ?: return@runCatching
                        val price = draft.priceText.toDoubleOrNull()
                        val fee = draft.feeQuantityText.toDoubleOrNull()

                        val result = uc.execute(
                            RegisterMovementCommand(
                                portfolioId = portfolioId,
                                walletId = walletId,
                                assetId = assetId,
                                type = draft.type.toDomain(),
                                quantity = qty,
                                price = price,
                                feeQuantity = fee,
                                timestamp = System.currentTimeMillis(),
                                notes = draft.notes
                            )
                        )

                        // Re-etiquetar id fake → id real
                        val realId = result.movementId.toString()
                        idMap[fallbackId] = result.movementId

                        if (realId != fallbackId) {
                            allRows = allRows.map { if (it.id == fallbackId) it.copy(id = realId) else it }
                            val s2 = _state.value.copy(rows = allRows)
                            _state.value = s2.copy(filteredRows = applyFilters(s2))
                        }
                    }
                }
            }

            MovementFormMode.EDIT -> {
                val uc = editMovement ?: return
                val movementId = draft.id?.toLongOrNull() ?: idMap[fallbackId] ?: return

                viewModelScope.launch {
                    runCatching {
                        val qty = draft.quantityText.toDoubleOrNull() ?: return@runCatching
                        val price = draft.priceText.toDoubleOrNull()
                        val fee = draft.feeQuantityText.toDoubleOrNull()

                        uc.execute(
                            EditMovementCommand(
                                movementId = movementId,
                                newType = draft.type.toDomain(),
                                newQuantity = qty,
                                newPrice = price,
                                newFeeQuantity = fee,
                                newTimestamp = System.currentTimeMillis(),
                                newNotes = draft.notes
                            )
                        )
                    }
                }
            }
        }
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

        // ---- UI first   ----
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

        //  ---- Side-effect productivo (SwapUseCase) ----
        val uc = swapMovement ?: return

        viewModelScope.launch {
            runCatching {
                val portfolioId = portfolioIdProvider()
                val walletId = walletIdResolver(draft.wallet) ?: return@runCatching
                val fromAssetId = assetIdResolver(draft.fromCrypto) ?: return@runCatching
                val toAssetId = assetIdResolver(draft.toCrypto) ?: return@runCatching

                val fromQty = draft.fromQtyText.toDoubleOrNull() ?: return@runCatching
                val toQty = draft.toQtyText.toDoubleOrNull() ?: return@runCatching

                uc.execute(
                    SwapMovementCommand(
                        portfolioId = portfolioId,
                        walletId = walletId,
                        fromAssetId = fromAssetId,
                        toAssetId = toAssetId,
                        fromQuantity = fromQty,
                        toQuantity = toQty,
                        timestamp = System.currentTimeMillis(),
                        notes = "" // draft no expone notes aquí sin tocar UX
                    )
                )
            }
        }
    }

    fun requestDelete(row: MovementRow) {
        _state.value = _state.value.copy(pendingDeleteId = row.id)
    }

    fun cancelDelete() {
        _state.value = _state.value.copy(pendingDeleteId = null)
    }

    fun confirmDelete(id: String) {
        val st = _state.value

        // ---- UI first ----
        allRows = allRows.filterNot { it.id == id }
        val next = st.copy(rows = allRows, pendingDeleteId = null)
        _state.value = next.copy(filteredRows = applyFilters(next))

        // ---- Side-effect productivo (solo si podemos resolver movementId Long) ----
        val uc = deleteMovement ?: return
        val movementId = id.toLongOrNull() ?: idMap[id] ?: return

        viewModelScope.launch {
            runCatching {
                uc.execute(DeleteMovementCommand(movementId))
            }
        }
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