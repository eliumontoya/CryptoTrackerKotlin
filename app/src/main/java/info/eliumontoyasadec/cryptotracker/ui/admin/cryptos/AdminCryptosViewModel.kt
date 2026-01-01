package info.eliumontoyasadec.cryptotracker.ui.admin.cryptos

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import info.eliumontoyasadec.cryptotracker.domain.model.Crypto
import info.eliumontoyasadec.cryptotracker.domain.repositories.CryptoRepository
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
    private val repo: CryptoRepository
) : ViewModel() {

    var state: AdminCryptosState = AdminCryptosState()
        private set

    fun load() {
        viewModelScope.launch {
            state = state.copy(loading = true, error = null, lastActionMessage = null)
            try {
                val items = repo.getAll()
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
        val symbol = state.draftSymbol.trim()
        val name = state.draftName.trim()

        if (symbol.isBlank()) {
            state = state.copy(error = "El símbolo es obligatorio.")
            return
        }
        if (name.isBlank()) {
            state = state.copy(error = "El nombre es obligatorio.")
            return
        }

        viewModelScope.launch {
            state = state.copy(loading = true, error = null, lastActionMessage = null)
            try {
                repo.upsertOne(Crypto(symbol = symbol, name = name,coingeckoId = null, isActive = state.draftActive))
                val items = repo.getAll()
                state = state.copy(
                    items = items,
                     showForm = false,
                    lastActionMessage = if (state.isEditing) "Crypto actualizada." else "Crypto creada."
                )
            } catch (t: Throwable) {
                state = state.copy(
                     error = t.message ?: "Fallo desconocido"
                )
            }finally {
                state = state.copy(loading = false)
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
            try {
                val deleted = repo.deleteBySymbol(symbol)
                val items = repo.getAll()
                state = state.copy(
                    items = items,
                    loading = false,
                    pendingDeleteSymbol = null,
                    lastActionMessage = if (deleted > 0) "Crypto eliminada." else "No se eliminó (no existía)."
                )
            } catch (t: Throwable) {
                // Muy probable FK constraint si hay holdings/movements referenciando la crypto
                state = state.copy(
                    loading = false,
                    pendingDeleteSymbol = null,
                    error = t.message ?: "No se pudo eliminar (posible relación con otros datos)."
                )
            }
        }
    }

    fun consumeLastActionMessage() {
        state = state.copy(lastActionMessage = null)
    }
}