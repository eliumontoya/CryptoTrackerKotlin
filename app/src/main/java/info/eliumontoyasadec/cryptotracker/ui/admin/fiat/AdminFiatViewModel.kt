package info.eliumontoyasadec.cryptotracker.ui.admin.fiat

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import info.eliumontoyasadec.cryptotracker.domain.model.Fiat
import info.eliumontoyasadec.cryptotracker.domain.repositories.FiatRepository
import kotlinx.coroutines.launch

data class AdminFiatState(
    val loading: Boolean = false,
    val error: String? = null,
    val items: List<Fiat> = emptyList(),

    // Form
    val showForm: Boolean = false,
    val editing: Fiat? = null,

    // Delete
    val showDeleteConfirm: Boolean = false,
    val pendingDelete: Fiat? = null
)

class AdminFiatViewModel(
    private val repo: FiatRepository
) : ViewModel() {

    var state by mutableStateOf(AdminFiatState())
        private set

    fun load() {
        viewModelScope.launch {
            state = state.copy(loading = true, error = null)
            val items = try {
                repo.getAll()
            } catch (t: Throwable) {
                state = state.copy(loading = false, error = t.message ?: "Fallo desconocido")
                return@launch
            }
            state = state.copy(items = items, loading = false)
        }
    }

    fun openCreate() {
        state = state.copy(showForm = true, editing = null, error = null)
    }

    fun openEdit(item: Fiat) {
        state = state.copy(showForm = true, editing = item, error = null)
    }

    fun dismissForm() {
        state = state.copy(showForm = false, editing = null)
    }

    fun save(code: String, name: String, symbol: String) {
        val c = code.trim()
        val n = name.trim()
         val s = symbol.trim().takeIf { it.isNotBlank() }

        if (c.isBlank()) {
            state = state.copy(error = "El código no puede estar vacío")
            return
        }
        if (n.isBlank()) {
            state = state.copy(error = "El nombre no puede estar vacío")
            return
        }

        viewModelScope.launch {
            state = state.copy(loading = true, error = null)
            try {
                repo.upsert(Fiat(code = c, name = n, symbol = s ?: ""))
                val items = repo.getAll()
                state = state.copy(items = items,  showForm = false, editing = null)
            } catch (t: Throwable) {
                state = state.copy(  error = t.message ?: "Fallo desconocido")
            }finally {
                state = state.copy(loading = false)
            }
        }
    }

    fun requestDelete(item: Fiat) {
        state = state.copy(showDeleteConfirm = true, pendingDelete = item)
    }

    fun cancelDelete() {
        state = state.copy(showDeleteConfirm = false, pendingDelete = null)
    }

    fun confirmDelete() {
        val target = state.pendingDelete ?: return
        viewModelScope.launch {
            state = state.copy(loading = true, error = null)
            try {
                repo.delete(target.code)
                val items = repo.getAll()
                state = state.copy(
                    items = items,
                     showDeleteConfirm = false,
                    pendingDelete = null
                )
            } catch (t: Throwable) {
                state = state.copy(  error = t.message ?: "Fallo desconocido")
            }finally {
                state = state.copy(loading = false)
            }

        }
    }
}