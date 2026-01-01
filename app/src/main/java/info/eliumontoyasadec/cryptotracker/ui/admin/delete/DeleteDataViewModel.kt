package info.eliumontoyasadec.cryptotracker.ui.admin.delete

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import info.eliumontoyasadec.cryptotracker.data.seed.DeleteRequest
import info.eliumontoyasadec.cryptotracker.data.seed.DeleteResult
import info.eliumontoyasadec.cryptotracker.data.seed.DatabaseWiper
import kotlinx.coroutines.launch

data class DeleteDataUiState(
    val all: Boolean = false,

    val cryptos: Boolean = true,
    val wallets: Boolean = true,
    val fiat: Boolean = true,
    val movements: Boolean = true,
    val holdings: Boolean = true,
    val portfolio: Boolean = true,

    val loading: Boolean = false,

    val showConfirm: Boolean = false,
    val pendingRequest: DeleteRequest? = null,

    val showResult: Boolean = false,
    val lastResult: DeleteResult? = null,
    val lastError: String? = null
) {
    val anySelected: Boolean
        get() = all || cryptos || wallets || fiat || movements || holdings || portfolio
}

sealed interface DeleteDataAction {
    data class ToggleAll(val value: Boolean) : DeleteDataAction
    data class ToggleMovements(val value: Boolean) : DeleteDataAction
    data class ToggleHoldings(val value: Boolean) : DeleteDataAction
    data class ToggleWallets(val value: Boolean) : DeleteDataAction
    data class TogglePortfolio(val value: Boolean) : DeleteDataAction
    data class ToggleCryptos(val value: Boolean) : DeleteDataAction
    data class ToggleFiat(val value: Boolean) : DeleteDataAction

    data object RequestDelete : DeleteDataAction
    data object CancelConfirm : DeleteDataAction
    data object ConfirmDelete : DeleteDataAction

    data object DismissResult : DeleteDataAction
}

class DeleteDataViewModel(
    private val wiper: DatabaseWiper
) : ViewModel() {

    var state: DeleteDataUiState = DeleteDataUiState()
        private set

    fun dispatch(action: DeleteDataAction) {
        when (action) {
            is DeleteDataAction.ToggleAll -> {
                val v = action.value
                state = if (v) {
                    state.copy(
                        all = true,
                        cryptos = true,
                        wallets = true,
                        fiat = true,
                        movements = true,
                        holdings = true,
                        portfolio = true
                    )
                } else {
                    state.copy(all = false)
                }
            }

            is DeleteDataAction.ToggleMovements -> state = state.copy(movements = action.value)
            is DeleteDataAction.ToggleHoldings -> state = state.copy(holdings = action.value)
            is DeleteDataAction.ToggleWallets -> state = state.copy(wallets = action.value)
            is DeleteDataAction.TogglePortfolio -> state = state.copy(portfolio = action.value)
            is DeleteDataAction.ToggleCryptos -> state = state.copy(cryptos = action.value)
            is DeleteDataAction.ToggleFiat -> state = state.copy(fiat = action.value)

            DeleteDataAction.RequestDelete -> openConfirm()
            DeleteDataAction.CancelConfirm -> closeConfirm()
            DeleteDataAction.ConfirmDelete -> confirmDelete()

            DeleteDataAction.DismissResult -> {
                state = state.copy(showResult = false, lastResult = null, lastError = null)
            }
        }
    }

    private fun buildRequest(): DeleteRequest = DeleteRequest(
        all = state.all,
        wallets = if (state.all) true else state.wallets,
        cryptos = if (state.all) true else state.cryptos,
        fiat = if (state.all) true else state.fiat,
        movements = if (state.all) true else state.movements,
        holdings = if (state.all) true else state.holdings,
        portfolio = if (state.all) true else state.portfolio
    )

    private fun openConfirm() {
        if (state.loading) return
        val req = buildRequest()
        state = state.copy(showConfirm = true, pendingRequest = req)
    }

    private fun closeConfirm() {
        if (state.loading) return
        state = state.copy(showConfirm = false, pendingRequest = null)
    }

    private fun confirmDelete() {
        val req = state.pendingRequest ?: return
        if (state.loading) return

        state = state.copy(showConfirm = false, loading = true)

        viewModelScope.launch {
            try {
                val res = wiper.wipe(req)
                state = state.copy(lastResult = res, lastError = null)
            } catch (t: Throwable) {
                state = state.copy(lastResult = null, lastError = t.message ?: "Fallo desconocido")
            } finally {
                state = state.copy(
                    loading = false,
                    pendingRequest = null,
                    showResult = true
                )
            }
        }
    }
}