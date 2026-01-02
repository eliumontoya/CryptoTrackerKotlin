package info.eliumontoyasadec.cryptotracker.ui.admin.wallets

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import info.eliumontoyasadec.cryptotracker.domain.interactor.wallet.DeleteWalletCommand
import info.eliumontoyasadec.cryptotracker.domain.interactor.wallet.DeleteWalletResult
import info.eliumontoyasadec.cryptotracker.domain.interactor.wallet.DeleteWalletUseCase
import info.eliumontoyasadec.cryptotracker.domain.interactor.wallet.GetWalletsByPortfolioCommand
import info.eliumontoyasadec.cryptotracker.domain.interactor.wallet.GetWalletsByPortfolioUseCase
import info.eliumontoyasadec.cryptotracker.domain.interactor.wallet.LoadAdminWalletsContextResult
import info.eliumontoyasadec.cryptotracker.domain.interactor.wallet.LoadAdminWalletsContextUseCase
import info.eliumontoyasadec.cryptotracker.domain.interactor.wallet.SetMainWalletCommand
import info.eliumontoyasadec.cryptotracker.domain.interactor.wallet.SetMainWalletResult
import info.eliumontoyasadec.cryptotracker.domain.interactor.wallet.SetMainWalletUseCase
import info.eliumontoyasadec.cryptotracker.domain.interactor.wallet.UpsertWalletCommand
import info.eliumontoyasadec.cryptotracker.domain.interactor.wallet.UpsertWalletResult
import info.eliumontoyasadec.cryptotracker.domain.interactor.wallet.UpsertWalletUseCase
import info.eliumontoyasadec.cryptotracker.domain.model.Portfolio
import info.eliumontoyasadec.cryptotracker.domain.model.Wallet
import kotlinx.coroutines.launch

data class AdminWalletsState(
    val loading: Boolean = false,
    val error: String? = null,
    val portfolios: List<Portfolio> = emptyList(),
    val selectedPortfolioId: Long? = null,
    val items: List<Wallet> = emptyList(),

    val showEditor: Boolean = false,
    val editorWalletId: Long? = null,
    val editorName: String = "",
    val editorMakeMain: Boolean = false
)

class AdminWalletsViewModel(
    private val loadContext: LoadAdminWalletsContextUseCase,
    private val getWalletsByPortfolio: GetWalletsByPortfolioUseCase,
    private val upsertWallet: UpsertWalletUseCase,
    private val deleteWallet: DeleteWalletUseCase,
    private val setMainWallet: SetMainWalletUseCase
) : ViewModel() {

    //    var state: AdminWalletsState = AdminWalletsState()
    var state by mutableStateOf(AdminWalletsState())
        private set

    fun start() {
        viewModelScope.launch {
            state = state.copy(loading = true, error = null)

            val newState = when (val result = loadContext.execute()) {
                is LoadAdminWalletsContextResult.Success ->
                    state.copy(
                        portfolios = result.portfolios,
                        selectedPortfolioId = result.selectedPortfolioId,
                        items = result.wallets
                    )

                is LoadAdminWalletsContextResult.Failure ->
                    state.copy(error = result.message)
            }

            state = newState.copy(loading = false)
        }
    }

    fun selectPortfolio(portfolioId: Long) {
        if (portfolioId == state.selectedPortfolioId) return

        state = state.copy(selectedPortfolioId = portfolioId)
        viewModelScope.launch {
            state = state.copy(loading = true, error = null)
            try {
                val wallets = getWalletsByPortfolio.execute(GetWalletsByPortfolioCommand(portfolioId))
                state = state.copy(items = wallets)
            } catch (t: Throwable) {
                state = state.copy(error = t.message ?: "Fallo desconocido")
            } finally {
                state = state.copy(loading = false)
            }
        }    }

    fun openCreate() {
        state = state.copy(
            showEditor = true,
            editorWalletId = null,
            editorName = "",
            editorMakeMain = false
        )
    }

    fun openEdit(item: Wallet) {
        state = state.copy(
            showEditor = true,
            editorWalletId = item.walletId,
            editorName = item.name,
            editorMakeMain = item.isMain
        )
    }

    fun closeEditor() {
        state = state.copy(showEditor = false)
    }

    fun onEditorNameChange(value: String) {
        state = state.copy(editorName = value)
    }

    fun onEditorMakeMainChange(value: Boolean) {
        state = state.copy(editorMakeMain = value)
    }

    fun saveEditor() {
        val portfolioId = state.selectedPortfolioId ?: return
        viewModelScope.launch {
            state = state.copy(loading = true, error = null)

            when (val result = upsertWallet.execute(
                UpsertWalletCommand(
                    portfolioId = portfolioId,
                    walletId = state.editorWalletId,
                    nameRaw = state.editorName,
                    makeMain = state.editorMakeMain
                )
            )) {
                is UpsertWalletResult.Success -> {
                    state = state.copy(items = result.items, showEditor = false)
                }
                is UpsertWalletResult.ValidationError -> {
                    state = state.copy(error = result.message)
                }
                is UpsertWalletResult.Failure -> {
                    state = state.copy(error = result.message)
                }
            }

            state = state.copy(loading = false)
        }
    }

    fun deleteWallet(walletId: Long) {
        val pid = state.selectedPortfolioId ?: return

        viewModelScope.launch {
            state = state.copy(loading = true, error = null)

            val newState = when (
                val result = deleteWallet.execute(DeleteWalletCommand(pid, walletId))
            ) {
                is DeleteWalletResult.Success ->
                    state.copy(items = result.items)

                is DeleteWalletResult.Failure ->
                    state.copy(items = result.items, error = result.message)
            }

            state = newState.copy(loading = false)
        }
    }
    fun makeMain(walletId: Long) {
        val pid = state.selectedPortfolioId ?: return

        viewModelScope.launch {
            state = state.copy(loading = true, error = null)

            val newState = when (
                val result = setMainWallet.execute(SetMainWalletCommand(pid, walletId))
            ) {
                is SetMainWalletResult.Success ->
                    state.copy(items = result.items)

                is SetMainWalletResult.Failure ->
                    state.copy(error = result.message)
            }

            state = newState.copy(loading = false)
        }
    }
}