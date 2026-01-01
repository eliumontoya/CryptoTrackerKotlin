package info.eliumontoyasadec.cryptotracker.ui.admin.load

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import info.eliumontoyasadec.cryptotracker.data.seed.CatalogSeeder
import info.eliumontoyasadec.cryptotracker.data.seed.CatalogStatus
import info.eliumontoyasadec.cryptotracker.data.seed.SeedRequest
import kotlinx.coroutines.launch

data class LoadInitialCatalogsUiState(
    val wallets: Boolean = true,
    val cryptos: Boolean = true,
    val fiat: Boolean = true,
    val syncManual: Boolean = false,

    val loading: Boolean = false,
    val lastResult: String? = null,
    val error: String? = null,

    val walletsAlready: Boolean = false,
    val cryptosAlready: Boolean = false,
    val fiatAlready: Boolean = false,

    val showConfirm: Boolean = false,
    val confirmText: String = ""
)

sealed interface LoadInitialCatalogsAction {
    data class ToggleWallets(val value: Boolean) : LoadInitialCatalogsAction
    data class ToggleCryptos(val value: Boolean) : LoadInitialCatalogsAction
    data class ToggleFiat(val value: Boolean) : LoadInitialCatalogsAction
    data class ToggleSyncManual(val value: Boolean) : LoadInitialCatalogsAction

    data object RequestSeed : LoadInitialCatalogsAction
    data object ConfirmSeed : LoadInitialCatalogsAction
    data object CancelSeed : LoadInitialCatalogsAction
}

class LoadInitialCatalogsViewModel(
    private val seeder: CatalogSeeder
) : ViewModel() {

    var state: LoadInitialCatalogsUiState = LoadInitialCatalogsUiState()
        private set

    private var status: CatalogStatus? = null
    private var pendingRequest: SeedRequest? = null

    init {
        refreshStatus()
    }

    fun dispatch(action: LoadInitialCatalogsAction) {
        when (action) {
            is LoadInitialCatalogsAction.ToggleWallets -> state = state.copy(wallets = action.value)
            is LoadInitialCatalogsAction.ToggleCryptos -> state = state.copy(cryptos = action.value)
            is LoadInitialCatalogsAction.ToggleFiat -> state = state.copy(fiat = action.value)
            is LoadInitialCatalogsAction.ToggleSyncManual -> state = state.copy(syncManual = action.value)

            LoadInitialCatalogsAction.RequestSeed -> requestSeed()
            LoadInitialCatalogsAction.CancelSeed -> cancelSeed()
            LoadInitialCatalogsAction.ConfirmSeed -> confirmSeed()
        }
    }

    private fun refreshStatus() {
        viewModelScope.launch {
            try {
                status = seeder.status()
                val s = status
                state = state.copy(
                    walletsAlready = (s?.wallets ?: 0) > 0,
                    cryptosAlready = (s?.cryptos ?: 0) > 0,
                    fiatAlready = (s?.fiat ?: 0) > 0,
                    error = null
                )
            } catch (t: Throwable) {
                state = state.copy(error = t.message ?: "Error obteniendo status")
            }
        }
    }

    private fun buildEffectiveRequest(): SeedRequest {
        return SeedRequest(
            wallets = state.wallets && !state.walletsAlready,
            cryptos = state.cryptos && !state.cryptosAlready,
            fiat = state.fiat && !state.fiatAlready,
            syncManual = state.syncManual
        )
    }

    private fun requestSeed() {
        val req = buildEffectiveRequest()
        pendingRequest = req

        val confirmText = buildString {
            appendLine("Se crearán datos predeterminados en la base de datos:")
            if (req.wallets) appendLine("• Portafolio + Carteras")
            if (req.cryptos) appendLine("• Catálogo de Cryptos")
            if (req.fiat) appendLine("• Catálogo FIAT")
            if (req.syncManual) appendLine("• Configuración Sync Manual")
        }.trim()

        state = state.copy(showConfirm = true, confirmText = confirmText, error = null)
    }

    private fun cancelSeed() {
        pendingRequest = null
        state = state.copy(showConfirm = false, confirmText = "")
    }

    private fun confirmSeed() {
        val req = pendingRequest ?: return
        pendingRequest = null

        state = state.copy(showConfirm = false, loading = true, lastResult = null, error = null)

        viewModelScope.launch {
            val resultText = try {
                val res = seeder.seed(req)
                buildString {
                    append("OK: ")
                    append("wallets=${res.walletsInserted}, ")
                    append("cryptos=${res.cryptosUpserted}, ")
                    append("fiat=${res.fiatUpserted}")
                }
            } catch (t: Throwable) {
                state = state.copy(error = t.message ?: "Fallo desconocido")
                "Error: ${t.message ?: "fallo desconocido"}"
            } finally {
                state = state.copy(loading = false)
            }

            state = state.copy(lastResult = resultText)
            refreshStatus()
        }
    }
}