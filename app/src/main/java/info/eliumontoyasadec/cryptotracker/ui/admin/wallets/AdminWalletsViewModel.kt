package info.eliumontoyasadec.cryptotracker.ui.admin.wallets

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import info.eliumontoyasadec.cryptotracker.domain.model.Portfolio
import info.eliumontoyasadec.cryptotracker.domain.model.Wallet
import info.eliumontoyasadec.cryptotracker.domain.repositories.PortfolioRepository
import info.eliumontoyasadec.cryptotracker.domain.repositories.WalletRepository
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
    private val walletRepo: WalletRepository,
    private val portfolioRepo: PortfolioRepository
) : ViewModel() {

    //    var state: AdminWalletsState = AdminWalletsState()
    var state by mutableStateOf(AdminWalletsState())
        private set

    fun start() {
        loadPortfolios()
    }

    fun selectPortfolio(portfolioId: Long) {
        if (portfolioId == state.selectedPortfolioId) return

        state = state.copy(selectedPortfolioId = portfolioId)
        loadWallets(portfolioId)
    }

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
        val name = state.editorName.trim()
        val descripton = ""

        if (name.isBlank()) {
            state = state.copy(error = "El nombre no puede estar vacío.")
            return
        }

        viewModelScope.launch {
            state = state.copy(loading = true, error = null)
            try {
                val editingId = state.editorWalletId
                if (editingId == null) {
                    val newId = walletRepo.insert(
                        Wallet(
                            walletId = 0L,
                            portfolioId = portfolioId,
                            name = name,
                            description = descripton,
                            isMain = state.editorMakeMain
                        )
                    )
                    if (state.editorMakeMain) walletRepo.setMain(newId)
                } else {
                    walletRepo.update(editingId, name)
                    if (state.editorMakeMain) walletRepo.setMain(editingId)
                }

                state = state.copy(showEditor = false)
                loadWallets(portfolioId) //  recarga con el portfolio actual
            } catch (t: Throwable) {
                state = state.copy(
                    loading = false,
                    error = t.message ?: "Fallo desconocido"
                )
            }
        }
    }

    fun deleteWallet(walletId: Long) {
        viewModelScope.launch {
            state = state.copy(loading = true, error = null)
            try {
                walletRepo.delete(walletId)
                val pid = state.selectedPortfolioId ?: return@launch
                loadWallets(pid) //  recarga con el portfolio actual
            } catch (t: Throwable) {
                state = state.copy(
                    loading = false,
                    error = t.message ?: "Fallo desconocido"
                )
            }
        }
    }

    fun makeMain(walletId: Long) {
        viewModelScope.launch {
            state = state.copy(loading = true, error = null)
            try {
                walletRepo.setMain(walletId)
                val pid = state.selectedPortfolioId ?: return@launch
                loadWallets(pid) //  recarga con el portfolio actual
            } catch (t: Throwable) {
                state = state.copy(
                    loading = false,
                    error = t.message ?: "Fallo desconocido"
                )
            }
        }
    }


    private fun loadPortfolios() {
        viewModelScope.launch {
            state = state.copy(loading = true, error = null)
            try {
                val portfolios = portfolioRepo.getAll()

                // 1) default primero
                val defaultId = portfolios.firstOrNull { it.isDefault }?.portfolioId
                val ordered = if (defaultId != null) {
                    portfolios.sortedByDescending { it.portfolioId == defaultId }
                } else {
                    portfolios
                }

                // 2) seleccion inicial: default o el primero
                val selectedId = defaultId ?: ordered.firstOrNull()?.portfolioId

                state = state.copy(
                    portfolios = ordered,
                    selectedPortfolioId = selectedId,
                    loading = false
                )

                if (selectedId != null) loadWallets(selectedId)
            } catch (t: Throwable) {
                state = state.copy(
                    loading = false,
                    error = t.message ?: "Fallo desconocido"
                )
            }
        }
    }

    private fun loadWallets(portfolioId: Long) {
        viewModelScope.launch {
            state = state.copy(loading = true, error = null)
            try {
                val wallets = walletRepo.getByPortfolio(portfolioId)
                state = state.copy(items = wallets)
            } catch (t: Throwable) {
                state = state.copy(
                    error = t.message ?: "Fallo desconocido"
                )
            } finally {
                state = state.copy(loading = false)
            }
        }
    }
}