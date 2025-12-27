package info.eliumontoyasadec.cryptotracker.ui.portfolio

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class PortfolioViewModel : ViewModel() {

    private val _state = MutableStateFlow(PortfolioFakeData.sample)
    val state: StateFlow<PortfolioUiState> = _state.asStateFlow()

    // UI-only, por ahora. Luego esto llamará a queries/repo.
    fun refreshFake() {
        _state.value = _state.value.copy(lastUpdatedLabel = "Actualizado (fake)")
    }
}