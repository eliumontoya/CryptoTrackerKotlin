package info.eliumontoyasadec.cryptotracker.ui.screens.movements

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import info.eliumontoyasadec.cryptotracker.domain.interactor.movement.DeleteMovementCommand
import info.eliumontoyasadec.cryptotracker.domain.interactor.movement.DeleteMovementUseCase
import info.eliumontoyasadec.cryptotracker.domain.interactor.movement.EditMovementCommand
import info.eliumontoyasadec.cryptotracker.domain.interactor.movement.EditMovementUseCase
import info.eliumontoyasadec.cryptotracker.domain.interactor.movement.LoadMovementsCommand
import info.eliumontoyasadec.cryptotracker.domain.interactor.movement.LoadMovementsUseCase
import info.eliumontoyasadec.cryptotracker.domain.interactor.movement.MoveBetweenWalletsUseCase
import info.eliumontoyasadec.cryptotracker.domain.interactor.movement.RegisterMovementCommand
import info.eliumontoyasadec.cryptotracker.domain.interactor.movement.RegisterMovementUseCase
import info.eliumontoyasadec.cryptotracker.domain.interactor.movement.SwapMovementCommand
import info.eliumontoyasadec.cryptotracker.domain.interactor.movement.SwapMovementUseCase
import info.eliumontoyasadec.cryptotracker.domain.model.Movement
import info.eliumontoyasadec.cryptotracker.domain.model.MovementType
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

class MovementsViewModel(
    private val mode: MovementMode,

    // UseCases (inyectados).
    private val loadMovements: LoadMovementsUseCase? = null,
    private val registerMovement: RegisterMovementUseCase? = null,
    private val editMovement: EditMovementUseCase? = null,
    private val deleteMovement: DeleteMovementUseCase? = null,
    private val swapMovement: SwapMovementUseCase? = null,

    //Todo: Aún no lo invocamos: falta UI suficiente para construir el command sin cambiar UX.
    private val moveBetweenWallets: MoveBetweenWalletsUseCase? = null,

    private val portfolioIdProvider: () -> Long,
    private val walletIdResolver: (WalletFilter) -> Long?,
    private val assetIdResolver: (CryptoFilter) -> String?
) : ViewModel() {

     private val tempIdToRealId = mutableMapOf<String, Long>()

    private var allRows: List<MovementRow> =
        if (loadMovements == null) fakeRowsFor(mode) else emptyList()

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


    init {
        if (loadMovements != null) {
            load()
        }
    }

    fun load() {
        val loader = loadMovements ?: return
        viewModelScope.launch {
            val pid = portfolioIdProvider()
            val items = loader.execute(LoadMovementsCommand(portfolioId = pid)).items

            val rows = mapMovementsToRows(items, mode)
            allRows = rows

            val next = _state.value.copy(rows = rows)
            _state.value = next.copy(filteredRows = applyFilters(next))
        }
    }

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
        val nowId = draft.id ?: "${mode.name.lowercase()}-${System.currentTimeMillis()}"
        val optimisticRow = draft.toRow(id = nowId, mode = mode)

        allRows = if (form.mode == MovementFormMode.CREATE) {
            listOf(optimisticRow) + allRows
        } else {
            allRows.map { if (it.id == nowId) optimisticRow else it }
        }

        val next = st.copy(rows = allRows, movementForm = null)
        _state.value = next.copy(filteredRows = applyFilters(next))

        // ---- Side-effect productivo (si hay UseCases y podemos construir command) ----

        viewModelScope.launch {
            when (form.mode) {
                MovementFormMode.CREATE -> registerIfPossible(nowId, draft)
                MovementFormMode.EDIT -> editIfPossible(nowId, draft)
            }
        }

    }

    private suspend fun registerIfPossible(tmpId: String, draft: MovementDraft) {
        val uc = registerMovement ?: return
        val pid = portfolioIdProvider()
        val wid = walletIdResolver(draft.wallet) ?: return
        val aid = assetIdResolver(draft.crypto) ?: return

        val qty = draft.quantityText.toDoubleOrNull() ?: return
        val fee = draft.feeQuantityText.toDoubleOrNull()
        val price = draft.priceText.toDoubleOrNull()

        val res = uc.execute(
            RegisterMovementCommand(
                portfolioId = pid,
                walletId = wid,
                assetId = aid,
                type = draft.type.toDomain(),
                quantity = qty,
                price = price,
                feeQuantity = fee,
                timestamp = draft.timestampOrNow(),
                notes = draft.notes
            )
        )

        tempIdToRealId[tmpId] = res.movementId
        relabelRowId(tmpId, res.movementId.toString())

        // Si hay load real, refrescamos desde DB para orden/consistencia
        if (loadMovements != null) load()
    }
    private fun relabelRowId(oldId: String, newId: String) {
        allRows = allRows.map { if (it.id == oldId) it.copy(id = newId) else it }
        val st = _state.value.copy(rows = allRows)
        _state.value = st.copy(filteredRows = applyFilters(st))
    }

    private suspend fun editIfPossible(rowId: String, draft: MovementDraft) {
        val uc = editMovement ?: return

        val movementId = rowId.toLongOrNull() ?: tempIdToRealId[rowId] ?: return

        val qty = draft.quantityText.toDoubleOrNull() ?: return
        val price = draft.priceText.toDoubleOrNull()
        val fee = draft.feeQuantityText.toDoubleOrNull()

        uc.execute(
            EditMovementCommand(
                movementId = movementId,
                newType = draft.type.toDomain(),
                newQuantity = qty,
                newPrice = price,
                newFeeQuantity = fee,
                newTimestamp = draft.timestampOrNow(),
                newNotes = draft.notes
            )
        )

        if (loadMovements != null) load()
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
                if (loadMovements != null) load()

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
                if (loadMovements != null) load()

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

    private fun MovementDraft.timestampOrNow(): Long {
        // Si en tu draft ya guardas timestamp real, úsalo.
        // Si no, caemos a now para no romper.
        return System.currentTimeMillis()
    }

    private fun mapMovementsToRows(items: List<Movement>, mode: MovementMode): List<MovementRow> {
        // Agrupaciones por groupId para SWAP/BETWEEN
        val byGroup = items.filter { it.groupId != null }.groupBy { it.groupId!! }
        val usedMovementIds = mutableSetOf<Long>()

        val rowsFromGroups = buildList {
            when (mode) {
                MovementMode.SWAP -> {
                    byGroup.forEach { (gid, list) ->
                        val buy = list.firstOrNull { it.type == MovementType.BUY }
                        val sell = list.firstOrNull { it.type == MovementType.SELL }
                        if (buy != null && sell != null) {
                            usedMovementIds += buy.id
                            usedMovementIds += sell.id

                            val wallet = walletFilterFromId(buy.walletId)
                            val toCrypto = cryptoFilterFromAssetId(buy.assetId)
                            val fromCrypto = cryptoFilterFromAssetId(sell.assetId)

                            add(
                                MovementRow(
                                    id = gid.toString(),
                                    dateLabel = formatDate(maxOf(buy.timestamp, sell.timestamp)),
                                    wallet = wallet,
                                    crypto = toCrypto,
                                    headline = "Swap en ${wallet.label}",
                                    details = "+ ${buy.quantity} ${toCrypto.label} · - ${sell.quantity} ${fromCrypto.label}"
                                )
                            )
                        }
                    }
                }

                MovementMode.BETWEEN -> {
                    byGroup.forEach { (gid, list) ->
                        val out = list.firstOrNull { it.type == MovementType.TRANSFER_OUT }
                        val `in` = list.firstOrNull { it.type == MovementType.TRANSFER_IN }
                        if (out != null && `in` != null) {
                            usedMovementIds += out.id
                            usedMovementIds += `in`.id

                            val fromW = walletFilterFromId(out.walletId)
                            val toW = walletFilterFromId(`in`.walletId)
                            val crypto = cryptoFilterFromAssetId(out.assetId)

                            add(
                                MovementRow(
                                    id = gid.toString(),
                                    dateLabel = formatDate(maxOf(out.timestamp, `in`.timestamp)),
                                    wallet = fromW,
                                    crypto = crypto,
                                    headline = "${fromW.label} → ${toW.label}",
                                    details = "${out.quantity} ${crypto.label}"
                                )
                            )
                        }
                    }
                }

                else -> Unit
            }
        }

        val filteredSingles = items
            .filter { it.id !in usedMovementIds }
            .filter { it.groupId == null } // evita que swaps/transfer se dupliquen
            .filter { movementMatchesMode(it, mode) }
            .map { m ->
                val wallet = walletFilterFromId(m.walletId)
                val crypto = cryptoFilterFromAssetId(m.assetId)
                MovementRow(
                    id = m.id.toString(),
                    dateLabel = formatDate(m.timestamp),
                    wallet = wallet,
                    crypto = crypto,
                    headline = headlineFor(m, crypto),
                    details = detailsFor(m)
                )
            }

        return (rowsFromGroups + filteredSingles).sortedByDescending { it.dateLabel } // simple
    }

    private fun movementMatchesMode(m: Movement, mode: MovementMode): Boolean {
        return when (mode) {
            MovementMode.IN -> m.type in setOf(MovementType.DEPOSIT, MovementType.TRANSFER_IN)
            MovementMode.OUT -> m.type in setOf(MovementType.WITHDRAW, MovementType.TRANSFER_OUT, MovementType.FEE)
            MovementMode.BETWEEN -> m.type in setOf(MovementType.TRANSFER_IN, MovementType.TRANSFER_OUT)
            MovementMode.SWAP -> m.type in setOf(MovementType.BUY, MovementType.SELL)
        }
    }

    private fun headlineFor(m: Movement, crypto: CryptoFilter): String {
        val qty = m.quantity
        return when (m.type) {
            MovementType.DEPOSIT, MovementType.TRANSFER_IN, MovementType.BUY -> "+ $qty ${crypto.label}"
            MovementType.WITHDRAW, MovementType.TRANSFER_OUT, MovementType.SELL, MovementType.FEE -> "- $qty ${crypto.label}"
            else -> "${m.type}"
        }
    }

    private fun detailsFor(m: Movement): String {
        return buildString {
            append(m.type.name)
            append(" · qty=").append(m.quantity)
            m.price?.let { append(" · price=").append(it) }
            if (m.feeQuantity != 0.0) append(" · fee=").append(m.feeQuantity)
            if (!m.notes.isNullOrBlank()) append(" · ").append(m.notes)
        }
    }

    private fun walletFilterFromId(walletId: Long): WalletFilter {
        return WalletFilter.entries
            .firstOrNull { it != WalletFilter.ALL && walletIdResolver(it) == walletId }
            ?: WalletFilter.ALL
    }

    private fun cryptoFilterFromAssetId(assetId: String): CryptoFilter {
        return CryptoFilter.entries
            .firstOrNull { it != CryptoFilter.ALL && assetIdResolver(it) == assetId }
            ?: CryptoFilter.ALL
    }

    private fun formatDate(ts: Long): String {
        val fmt = DateTimeFormatter.ofPattern("dd MMM yyyy", Locale("es", "MX"))
        return Instant.ofEpochMilli(ts).atZone(ZoneId.systemDefault()).toLocalDate().format(fmt)
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