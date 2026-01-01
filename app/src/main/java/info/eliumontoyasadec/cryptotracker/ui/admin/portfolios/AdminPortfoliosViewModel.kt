package info.eliumontoyasadec.cryptotracker.ui.admin.portfolios

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import info.eliumontoyasadec.cryptotracker.domain.model.Portfolio
import info.eliumontoyasadec.cryptotracker.domain.repositories.PortfolioRepository
import kotlinx.coroutines.launch

data class AdminPortfoliosUiState(
    val loading: Boolean = false,
    val items: List<Portfolio> = emptyList(),
    val error: String? = null
)

class AdminPortfoliosViewModel(
    private val repo: PortfolioRepository
) : ViewModel() {

    var state by mutableStateOf(AdminPortfoliosUiState())
        private set

    fun load() {
        viewModelScope.launch {
            state = state.copy(loading = true, error = null)
            try {
                state = state.copy(items = repo.getAll(), loading = false)
            } catch (t: Throwable) {
                state = state.copy(
                    loading = false,
                    error = t.message ?: "Fallo desconocido"
                )
            }finally {
                state = state.copy(loading = false)
            }
        }
    }

    fun create(
        name: String,
        description: String?,
        makeDefault: Boolean,
        onDone: () -> Unit
    ) {
        viewModelScope.launch {
            state = state.copy(loading = true, error = null)
            try {
                repo.insert(
                    Portfolio(
                    name = name,
                    description = description,
                    isDefault = makeDefault
                    )
                )
                state = state.copy(items = repo.getAll(), loading = false)
                onDone()
            } catch (t: Throwable) {
                state = state.copy(
                    loading = false,
                    error = t.message ?: "Fallo desconocido"
                )
            }
        }
    }

    fun update(
        id: Long,
        name: String,
        description: String?,
        makeDefault: Boolean,
        onDone: () -> Unit
    ) {
        viewModelScope.launch {
            state = state.copy(loading = true, error = null)
            try {
                repo.update(
                    Portfolio(
                    portfolioId = id,
                    name = name,
                    description = description,
                    isDefault = makeDefault)
                )
                state = state.copy(items = repo.getAll(), loading = false)
                onDone()
            } catch (t: Throwable) {
                state = state.copy(
                    loading = false,
                    error = t.message ?: "Fallo desconocido"
                )
            }
        }
    }

    fun delete(
        id: Long,
        onDone: () -> Unit
    ) {
        viewModelScope.launch {
            state = state.copy(loading = true, error = null)
            try {
                repo.delete(id)
                state = state.copy(items = repo.getAll(), loading = false)
                onDone()
            } catch (t: Throwable) {
                state = state.copy(
                    loading = false,
                    error = t.message ?: "Fallo desconocido"
                )
            }
        }
    }

    fun setDefault(
        id: Long
    ) {
        viewModelScope.launch {
            state = state.copy(loading = true, error = null)
            try {
                // Si el repo ya valida “si ya es default, no hace nada”, mejor.
                repo.setDefault(id)
                state = state.copy(items = repo.getAll() )
            } catch (t: Throwable) {
                state = state.copy(
                     error = t.message ?: "Fallo desconocido"
                )
            }
            finally {
                state = state.copy( loading = false)
            }
        }
    }

}