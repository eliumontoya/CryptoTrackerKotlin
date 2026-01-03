package info.eliumontoyasadec.cryptotracker.ui.admin.fiat

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import info.eliumontoyasadec.cryptotracker.domain.interactor.fiat.DeleteFiatCommand
import info.eliumontoyasadec.cryptotracker.domain.interactor.fiat.DeleteFiatUseCase
import info.eliumontoyasadec.cryptotracker.domain.interactor.fiat.GetAllFiatsUseCase
import info.eliumontoyasadec.cryptotracker.domain.interactor.fiat.UpsertFiatCommand
import info.eliumontoyasadec.cryptotracker.domain.interactor.fiat.UpsertFiatResult
import info.eliumontoyasadec.cryptotracker.domain.interactor.fiat.UpsertFiatUseCase
import info.eliumontoyasadec.cryptotracker.domain.model.Fiat
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
    private val getAllFiats: GetAllFiatsUseCase,
    private val upsertFiat: UpsertFiatUseCase,
    private val deleteFiat: DeleteFiatUseCase
) : ViewModel() {

    var state by mutableStateOf(AdminFiatState())

    fun load() {
        viewModelScope.launch {
            state = state.copy(loading = true, error = null)
            try {
                val items = getAllFiats.execute()
                state = state.copy(items = items)
            } catch (t: Throwable) {
                state = state.copy(error = t.message ?: "Fallo desconocido")
            } finally {
                state = state.copy(loading = false)
            }
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
        val isEditing = state.editing != null

        viewModelScope.launch {
            state = state.copy(loading = true, error = null)

            when (val result = upsertFiat.execute(
                UpsertFiatCommand(
                    codeRaw = code,
                    nameRaw = name,
                    symbolRaw = symbol,
                    isEditing = isEditing
                )
            )) {
                is UpsertFiatResult.Success -> {
                    state = state.copy(
                        items = result.items,
                        showForm = false,
                        editing = null
                    )
                }
                is UpsertFiatResult.ValidationError -> {
                    state = state.copy(error = result.message)
                }
                is UpsertFiatResult.Failure -> {
                    state = state.copy(error = result.message)
                }
            }

            state = state.copy(loading = false)
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

            val result = deleteFiat.execute(DeleteFiatCommand(target.code))
            state = when (result) {
                is info.eliumontoyasadec.cryptotracker.domain.interactor.fiat.DeleteFiatResult.Deleted ->
                    state.copy(items = result.items, showDeleteConfirm = false, pendingDelete = null)
                is info.eliumontoyasadec.cryptotracker.domain.interactor.fiat.DeleteFiatResult.NotFound ->
                    state.copy(items = result.items, showDeleteConfirm = false, pendingDelete = null)
                is info.eliumontoyasadec.cryptotracker.domain.interactor.fiat.DeleteFiatResult.Failure ->
                    state.copy(items = result.items, showDeleteConfirm = false, pendingDelete = null, error = result.message)
            }

            state = state.copy(loading = false)
        }
    }
}