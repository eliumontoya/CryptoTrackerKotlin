package info.eliumontoyasadec.cryptotracker.ui.screens.movements

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import info.eliumontoyasadec.cryptotracker.domain.interactor.movement.DeleteMovementCommand
import info.eliumontoyasadec.cryptotracker.domain.interactor.movement.DeleteMovementUseCase
import info.eliumontoyasadec.cryptotracker.domain.interactor.movement.EditMovementCommand
import info.eliumontoyasadec.cryptotracker.domain.interactor.movement.EditMovementUseCase
import info.eliumontoyasadec.cryptotracker.domain.interactor.movement.LoadMovementsCommand
import info.eliumontoyasadec.cryptotracker.domain.interactor.movement.LoadMovementsUseCase
import info.eliumontoyasadec.cryptotracker.domain.interactor.movement.RegisterMovementCommand
import info.eliumontoyasadec.cryptotracker.domain.interactor.movement.RegisterMovementUseCase
import info.eliumontoyasadec.cryptotracker.domain.interactor.movement.SwapMovementCommand
import info.eliumontoyasadec.cryptotracker.domain.interactor.movement.SwapMovementUseCase
import info.eliumontoyasadec.cryptotracker.domain.model.Movement
import info.eliumontoyasadec.cryptotracker.domain.model.MovementError
import info.eliumontoyasadec.cryptotracker.domain.model.MovementType
import info.eliumontoyasadec.cryptotracker.domain.model.Wallet
import info.eliumontoyasadec.cryptotracker.domain.repositories.PortfolioRepository
import info.eliumontoyasadec.cryptotracker.domain.repositories.WalletRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

class MovementsViewModel(
    private val mode: MovementMode,
    private val loadMovements: LoadMovementsUseCase,
    private val registerMovement: RegisterMovementUseCase? = null,
    private val editMovement: EditMovementUseCase? = null,
    private val deleteMovement: DeleteMovementUseCase? = null,
    private val swapMovement: SwapMovementUseCase? = null,
    private val portfolioRepo: PortfolioRepository,
    private val walletRepo: WalletRepository,
    private val assetIdResolver: (CryptoFilter) -> String? = { if (it == CryptoFilter.ALL) null else it.label }
) : ViewModel() {

    private val tempIdToRealId = mutableMapOf<String, Long>()
    private var allRows: List<MovementRow> = emptyList()

    private var currentPortfolioId: Long? = null
    private var cachedWallets: List<Wallet> = emptyList()
    private var walletNameById: Map<Long, String> = emptyMap()

    private val _state = MutableStateFlow(
        MovementsUiState(
            wallets = emptyList(),
            selectedWalletId = null,
            selectedCrypto = CryptoFilter.ALL,
            rows = emptyList(),
            filteredRows = emptyList(),
            movementForm = null,
            swapForm = null,
            pendingDeleteId = null,
            error = null
        )
    )
    val state: StateFlow<MovementsUiState> = _state.asStateFlow()

    private var loadJob: Job? = null

    init {
        viewModelScope.launch { bootstrap() }
    }

    private suspend fun bootstrap() {
        val p = portfolioRepo.getDefault()
        if (p == null) {
            _state.update { it.copy(error = "No existe portafolio default. Crea uno y márcalo como default.") }
            return
        }
        currentPortfolioId = p.portfolioId

        cachedWallets = walletRepo.getByPortfolio(p.portfolioId)
        walletNameById = cachedWallets.associate { it.walletId to it.name }

        _state.update { it.copy(wallets = cachedWallets) }

        load()
    }

    fun clearError() {
        _state.update { it.copy(error = null) }
    }

    fun load() {
        loadJob?.cancel()
        loadJob = viewModelScope.launch {
            val pid = currentPortfolioId ?: return@launch

            runCatching {
                loadMovements.execute(LoadMovementsCommand(portfolioId = pid)).items
            }.onSuccess { items ->
                val rows = mapMovementsToRows(items, mode)
                allRows = rows
                _state.update { st ->
                    val next = st.copy(rows = rows)
                    next.copy(filteredRows = applyFilters(next))
                }
            }.onFailure { t ->
                allRows = emptyList()
                _state.update { st ->
                    val next = st.copy(rows = emptyList(), error = t.message)
                    next.copy(filteredRows = applyFilters(next))
                }
            }
        }
    }

    fun selectWallet(walletId: Long?) {
        val next = _state.value.copy(selectedWalletId = walletId)
        _state.value = next.copy(filteredRows = applyFilters(next))
    }

    fun selectCrypto(crypto: CryptoFilter) {
        val next = _state.value.copy(selectedCrypto = crypto)
        _state.value = next.copy(filteredRows = applyFilters(next))
    }

    fun startCreate() {
        val st = _state.value
        if (mode == MovementMode.SWAP) {
            // asumo que SwapDraft ya tiene walletId (Long?) o wallet (Long?)
            _state.value = st.copy(
                swapForm = SwapFormState(SwapDraft()),
                movementForm = null
            )
        } else {
            val defaultWalletId = st.wallets.firstOrNull { it.isMain }?.walletId ?: st.wallets.firstOrNull()?.walletId
            _state.value = st.copy(
                movementForm = MovementFormState(
                    mode = MovementFormMode.CREATE,
                    draft = MovementDraft(
                        walletId = defaultWalletId,
                        type = defaultTypeForMode(mode)
                    )
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

        val nowId = draft.id ?: "${mode.name.lowercase()}-${System.currentTimeMillis()}"
        val optimisticRow = draft.toRow(id = nowId, mode = mode)

        allRows = if (form.mode == MovementFormMode.CREATE) {
            listOf(optimisticRow) + allRows
        } else {
            allRows.map { if (it.id == nowId) optimisticRow else it }
        }

        val next = st.copy(rows = allRows, movementForm = null)
        _state.value = next.copy(filteredRows = applyFilters(next))

        viewModelScope.launch {
            when (form.mode) {
                MovementFormMode.CREATE -> registerIfPossible(nowId, draft)
                MovementFormMode.EDIT -> editIfPossible(nowId, draft)
            }
        }
    }

    private suspend fun registerIfPossible(tmpId: String, draft: MovementDraft) {
        val uc = registerMovement ?: return

        val wid = draft.walletId ?: run {
            _state.update { it.copy(error = "Selecciona una wallet.") }
            return
        }

        val wallet = cachedWallets.firstOrNull { it.walletId == wid }
            ?: walletRepo.findById(wid)
            ?: run {
                _state.update { it.copy(error = "La wallet seleccionada no existe.") }
                return
            }

        val pid = wallet.portfolioId
        val aid = assetIdResolver(draft.crypto) ?: return

        val qty = draft.quantityText.toDoubleOrNull() ?: return
        val fee = draft.feeQuantityText.toDoubleOrNull()
        val price = draft.priceText.toDoubleOrNull()

        runCatching {
            uc.execute(
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
        }.onSuccess { res ->
            tempIdToRealId[tmpId] = res.movementId
            relabelRowId(tmpId, res.movementId.toString())
            load()
        }.onFailure { t ->
            val msg = when (t) {
                is MovementError.NotFound -> t.message ?: "No encontrado"
                else -> t.message ?: "Error al registrar movimiento"
            }
            _state.update { it.copy(error = msg) }
            load()
        }
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

        runCatching {
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
        }.onFailure { t ->
            _state.update { it.copy(error = t.message ?: "Error al editar movimiento") }
        }

        load()
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

         val wid = draft.id ?: run {
            _state.update { it.copy(error = "Selecciona una wallet.") }
            return
        }
        val wName = walletNameById[wid] ?: "Wallet $wid"

        val id = draft.id ?: "swap-${System.currentTimeMillis()}"
        val headline = "Swap en $wName"
        val details =
            "+ ${draft.toQtyText.ifBlank { "?" }} ${draft.toCrypto.label} · - ${draft.fromQtyText.ifBlank { "?" }} ${draft.fromCrypto.label}"

        val row = MovementRow(
            id = id.toString(),
            dateLabel = draft.dateLabel,
            walletId = wid,
            walletName = wName,
            crypto = draft.toCrypto,
            headline = headline,
            details = details
        )

        allRows = listOf(row) + allRows
        val next = st.copy(rows = allRows, swapForm = null)
        _state.value = next.copy(filteredRows = applyFilters(next))

        val uc = swapMovement ?: return

        viewModelScope.launch {
            val wallet = cachedWallets.firstOrNull { it.walletId == wid } ?: walletRepo.findById(wid) ?: return@launch
            val pid = wallet.portfolioId

            val fromAssetId = assetIdResolver(draft.fromCrypto) ?: return@launch
            val toAssetId = assetIdResolver(draft.toCrypto) ?: return@launch
            val fromQty = draft.fromQtyText.toDoubleOrNull() ?: return@launch
            val toQty = draft.toQtyText.toDoubleOrNull() ?: return@launch

            runCatching {
                uc.execute(
                    SwapMovementCommand(
                        portfolioId = pid,
                        walletId = wid,
                        fromAssetId = fromAssetId,
                        toAssetId = toAssetId,
                        fromQuantity = fromQty,
                        toQuantity = toQty,
                        timestamp = System.currentTimeMillis(),
                        notes = ""
                    )
                )
            }.onFailure { t ->
                _state.update { it.copy(error = t.message ?: "Error al guardar swap") }
            }

            load()
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
        allRows = allRows.filterNot { it.id == id }
        val next = st.copy(rows = allRows, pendingDeleteId = null)
        _state.value = next.copy(filteredRows = applyFilters(next))

        val uc = deleteMovement ?: return
        val movementId = id.toLongOrNull() ?: tempIdToRealId[id] ?: return

        viewModelScope.launch {
            runCatching { uc.execute(DeleteMovementCommand(movementId)) }
                .onFailure { t -> _state.update { it.copy(error = t.message ?: "Error al borrar") } }
            load()
        }
    }

    private fun applyFilters(state: MovementsUiState): List<MovementRow> {
        return state.rows.filter { row ->
            (state.selectedWalletId == null || row.walletId == state.selectedWalletId) &&
                    (state.selectedCrypto == CryptoFilter.ALL || row.crypto == state.selectedCrypto)
        }
    }

    private fun defaultTypeForMode(mode: MovementMode): MovementTypeUi = when (mode) {
        MovementMode.IN -> MovementTypeUi.DEPOSIT
        MovementMode.OUT -> MovementTypeUi.WITHDRAW
        MovementMode.BETWEEN -> MovementTypeUi.TRANSFER_OUT
        MovementMode.SWAP -> MovementTypeUi.BUY
    }

    private fun MovementRow.toDraft(): MovementDraft {
        val qty = headline.split(" ").getOrNull(1).orEmpty()
        return MovementDraft(
            id = id,
            walletId = walletId,
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
        val wName = walletId?.let { walletNameById[it] } ?: "(sin wallet)"
        val head = when (mode) {
            MovementMode.IN -> "+ $qty ${crypto.label}"
            MovementMode.OUT -> "- $qty ${crypto.label}"
            MovementMode.BETWEEN -> "$wName → (otra)"
            MovementMode.SWAP -> "Movimiento"
        }
        val det = buildString {
            append(type.label)
            append(" · qty=").append(qty)
            if (priceText.isNotBlank()) append(" · price=").append(priceText)
            if (feeQuantityText.isNotBlank()) append(" · fee=").append(feeQuantityText)
            if (notes.isNotBlank()) append(" · ").append(notes)
        }
        return MovementRow(
            id = id,
            dateLabel = dateLabel,
            walletId = walletId,
            walletName = wName,
            crypto = crypto,
            headline = head,
            details = det
        )
    }

    private fun MovementDraft.timestampOrNow(): Long = System.currentTimeMillis()

    private fun mapMovementsToRows(items: List<Movement>, mode: MovementMode): List<MovementRow> {
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

                            val wid = buy.walletId
                            val wName = walletNameById[wid] ?: "Wallet $wid"
                            val toCrypto = cryptoFilterFromAssetId(buy.assetId)
                            val fromCrypto = cryptoFilterFromAssetId(sell.assetId)

                            add(
                                MovementRow(
                                    id = gid.toString(),
                                    dateLabel = formatDate(maxOf(buy.timestamp, sell.timestamp)),
                                    walletId = wid,
                                    walletName = wName,
                                    crypto = toCrypto,
                                    headline = "Swap en $wName",
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

                            val fromId = out.walletId
                            val toId = `in`.walletId
                            val fromName = walletNameById[fromId] ?: "Wallet $fromId"
                            val toName = walletNameById[toId] ?: "Wallet $toId"
                            val crypto = cryptoFilterFromAssetId(out.assetId)

                            add(
                                MovementRow(
                                    id = gid.toString(),
                                    dateLabel = formatDate(maxOf(out.timestamp, `in`.timestamp)),
                                    walletId = fromId,
                                    walletName = fromName,
                                    crypto = crypto,
                                    headline = "$fromName → $toName",
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
            .filter { it.groupId == null }
            .filter { movementMatchesMode(it, mode) }
            .map { m ->
                val wid = m.walletId
                val wName = walletNameById[wid] ?: "Wallet $wid"
                val crypto = cryptoFilterFromAssetId(m.assetId)
                MovementRow(
                    id = m.id.toString(),
                    dateLabel = formatDate(m.timestamp),
                    walletId = wid,
                    walletName = wName,
                    crypto = crypto,
                    headline = headlineFor(m, crypto),
                    details = detailsFor(m)
                )
            }

        return (rowsFromGroups + filteredSingles)
    }

    private fun movementMatchesMode(m: Movement, mode: MovementMode): Boolean = when (mode) {
        MovementMode.IN -> m.type in setOf(MovementType.DEPOSIT, MovementType.TRANSFER_IN)
        MovementMode.OUT -> m.type in setOf(MovementType.WITHDRAW, MovementType.TRANSFER_OUT, MovementType.FEE)
        MovementMode.BETWEEN -> m.type in setOf(MovementType.TRANSFER_IN, MovementType.TRANSFER_OUT)
        MovementMode.SWAP -> m.type in setOf(MovementType.BUY, MovementType.SELL)
    }

    private fun headlineFor(m: Movement, crypto: CryptoFilter): String {
        val qty = m.quantity
        return when (m.type) {
            MovementType.DEPOSIT, MovementType.TRANSFER_IN, MovementType.BUY -> "+ $qty ${crypto.label}"
            MovementType.WITHDRAW, MovementType.TRANSFER_OUT, MovementType.SELL, MovementType.FEE -> "- $qty ${crypto.label}"
            else -> "${m.type}"
        }
    }

    private fun detailsFor(m: Movement): String = buildString {
        append(m.type.name)
        append(" · qty=").append(m.quantity)
        m.price?.let { append(" · price=").append(it) }
        if (m.feeQuantity != 0.0) append(" · fee=").append(m.feeQuantity)
        if (!m.notes.isNullOrBlank()) append(" · ").append(m.notes)
    }

    private fun formatDate(ts: Long): String {
        val fmt = DateTimeFormatter.ofPattern("yyyy-MM-dd").withLocale(Locale.getDefault())
        return Instant.ofEpochMilli(ts).atZone(ZoneId.systemDefault()).toLocalDate().format(fmt)
    }

    private fun cryptoFilterFromAssetId(assetId: String): CryptoFilter {
        return CryptoFilter.entries.firstOrNull { it != CryptoFilter.ALL && it.label == assetId } ?: CryptoFilter.ALL
    }
}