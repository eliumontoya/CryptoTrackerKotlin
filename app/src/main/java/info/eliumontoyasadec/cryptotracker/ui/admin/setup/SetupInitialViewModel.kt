package info.eliumontoyasadec.cryptotracker.ui.admin.setup

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch

data class SetupInitialUiState(
    val loading: Boolean = false,
    val error: String? = null,
    val lastActionMessage: String? = null,
    val confirmDeleteAll: Boolean = false
)

sealed interface SetupInitialAction {
    data object RequestDeleteAll : SetupInitialAction
    data object ConfirmDeleteAll : SetupInitialAction
    data object CancelDeleteAll : SetupInitialAction

    data object LoadInitialCatalogs : SetupInitialAction
    data object LoadInitialMovements : SetupInitialAction
    data object BackupExport : SetupInitialAction
    data object BackupImport : SetupInitialAction
}

data class SetupInitialOps(
    val deleteAllData: suspend () -> Unit,
    val loadInitialCatalogs: suspend () -> Unit,
    val loadInitialMovements: suspend () -> Unit,
    val backupExport: suspend () -> Unit,
    val backupImport: suspend () -> Unit
)

class SetupInitialViewModel(
    private val ops: SetupInitialOps
) : ViewModel() {

    var state: SetupInitialUiState = SetupInitialUiState()
        private set

    fun dispatch(action: SetupInitialAction) {
        when (action) {
            SetupInitialAction.RequestDeleteAll -> {
                state = state.copy(confirmDeleteAll = true, error = null, lastActionMessage = null)
            }
            SetupInitialAction.CancelDeleteAll -> {
                state = state.copy(confirmDeleteAll = false)
            }
            SetupInitialAction.ConfirmDeleteAll -> runOp(
                before = { state = state.copy(confirmDeleteAll = false) },
                op = { ops.deleteAllData() },
                okMsg = null
            )

            SetupInitialAction.LoadInitialCatalogs -> runOp(
                op = { ops.loadInitialCatalogs() },
                okMsg = null
            )
            SetupInitialAction.LoadInitialMovements -> runOp(
                op = { ops.loadInitialMovements() },
                okMsg = null
            )
            SetupInitialAction.BackupExport -> runOp(
                op = { ops.backupExport() },
                okMsg = null
            )
            SetupInitialAction.BackupImport -> runOp(
                op = { ops.backupImport() },
                okMsg = null
            )
        }
    }

    private fun runOp(
        before: (() -> Unit)? = null,
        op: suspend () -> Unit,
        okMsg: String?
    ) {
        if (state.loading) return
        before?.invoke()

        viewModelScope.launch {
            state = state.copy(loading = true, error = null, lastActionMessage = null)
            try {
                op()
                state = state.copy(lastActionMessage = okMsg)
            } catch (t: Throwable) {
                state = state.copy(error = t.message ?: "Fallo desconocido")
            } finally {
                state = state.copy(loading = false)
            }
        }
    }

}