package info.eliumontoyasadec.cryptotracker.ui.admin.cryptos

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import info.eliumontoyasadec.cryptotracker.domain.interactor.crypto.DeleteCryptoCommand
import info.eliumontoyasadec.cryptotracker.domain.interactor.crypto.DeleteCryptoUseCase
import info.eliumontoyasadec.cryptotracker.domain.interactor.crypto.GetAllCryptosUseCase
import info.eliumontoyasadec.cryptotracker.domain.interactor.crypto.UpsertCryptoCommand
import info.eliumontoyasadec.cryptotracker.domain.interactor.crypto.UpsertCryptoResult
import info.eliumontoyasadec.cryptotracker.domain.interactor.crypto.UpsertCryptoUseCase
import info.eliumontoyasadec.cryptotracker.domain.model.Crypto
import kotlinx.coroutines.launch

data class AdminCryptosState(
    val loading: Boolean = false,
    val items: List<Crypto> = emptyList(),
    val error: String? = null,

    val showForm: Boolean = false,
    val isEditing: Boolean = false,
    val draftSymbol: String = "",
    val draftName: String = "",
    val draftActive: Boolean = true,

    val pendingDeleteSymbol: String? = null,

    val lastActionMessage: String? = null
)

class AdminCryptosViewModel(
    private val getAllCryptos: GetAllCryptosUseCase,
    private val upsertCrypto: UpsertCryptoUseCase,
    private val deleteCrypto: DeleteCryptoUseCase
) : ViewModel() {

    var state: AdminCryptosState = AdminCryptosState()
        private set

    fun load() {
        viewModelScope.launch {
            state = state.copy(loading = true, error = null, lastActionMessage = null)
            try {
                val items = getAllCryptos.execute()
                state = state.copy(items = items )
            } catch (t: Throwable) {
                state = state.copy(
                     error = t.message ?: "Fallo desconocido"
                )
            }finally {
                state = state.copy(loading = false)
            }
        }
    }

    fun openCreate() {
        state = state.copy(
            showForm = true,
            isEditing = false,
            draftSymbol = "",
            draftName = "",
            draftActive = true,
            error = null,
            lastActionMessage = null
        )
    }

    fun openEdit(item: Crypto) {
        state = state.copy(
            showForm = true,
            isEditing = true,
            draftSymbol = item.symbol,
            draftName = item.name,
            draftActive = item.isActive,
            error = null,
            lastActionMessage = null
        )
    }

    fun dismissForm() {
        state = state.copy(showForm = false, error = null)
    }

    fun onDraftSymbolChange(v: String) {
        if (state.isEditing) return // símbolo no se edita (PK)
        state = state.copy(draftSymbol = v)
    }

    fun onDraftNameChange(v: String) {
        state = state.copy(draftName = v)
    }

    fun onDraftActiveChange(v: Boolean) {
        state = state.copy(draftActive = v)
    }

    fun save() {
        viewModelScope.launch {
            state = state.copy(loading = true, error = null, lastActionMessage = null)

            val result = upsertCrypto.execute(
                UpsertCryptoCommand(
                    symbolRaw = state.draftSymbol,
                    nameRaw = state.draftName,
                    isActive = state.draftActive,
                    isEditing = state.isEditing
                )
            )

            when (result) {
                is UpsertCryptoResult.Success -> {
                    state = state.copy(
                        items = result.items,
                        showForm = false,
                        lastActionMessage = if (result.wasUpdate) "Crypto actualizada." else "Crypto creada.",
                        loading = false
                    )
                }
                is UpsertCryptoResult.ValidationError -> {
                    state = state.copy(error = result.message, loading = false)
                }
                is UpsertCryptoResult.Failure -> {
                    state = state.copy(error = result.message, loading = false)
                }
            }
        }
    }

    fun requestDelete(symbol: String) {
        state = state.copy(pendingDeleteSymbol = symbol, error = null, lastActionMessage = null)
    }

    fun cancelDelete() {
        state = state.copy(pendingDeleteSymbol = null)
    }

    fun confirmDelete() {
        val symbol = state.pendingDeleteSymbol ?: return

        viewModelScope.launch {
            state = state.copy(loading = true, error = null, lastActionMessage = null)

            when (val result = deleteCrypto.execute(DeleteCryptoCommand(symbol))) {
                is info.eliumontoyasadec.cryptotracker.domain.interactor.crypto.DeleteCryptoResult.Deleted -> {
                    state = state.copy(
                        items = result.items,
                        pendingDeleteSymbol = null,
                        lastActionMessage = "Crypto eliminada.",
                        loading = false
                    )
                }
                is info.eliumontoyasadec.cryptotracker.domain.interactor.crypto.DeleteCryptoResult.NotFound -> {
                    state = state.copy(
                        items = result.items,
                        pendingDeleteSymbol = null,
                        lastActionMessage = "No se eliminó (no existía).",
                        loading = false
                    )
                }
                is info.eliumontoyasadec.cryptotracker.domain.interactor.crypto.DeleteCryptoResult.InUse -> {
                    state = state.copy(
                        items = result.items,
                        pendingDeleteSymbol = null,
                        error = result.message,
                        loading = false
                    )
                }
                is info.eliumontoyasadec.cryptotracker.domain.interactor.crypto.DeleteCryptoResult.Failure -> {
                    state = state.copy(
                        items = result.items,
                        pendingDeleteSymbol = null,
                        error = result.message,
                        loading = false
                    )
                }
            }
        }
    }

    fun consumeLastActionMessage() {
        state = state.copy(lastActionMessage = null)
    }
}