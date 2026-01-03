package info.eliumontoyasadec.cryptotracker.ui.admin.portfolios

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import info.eliumontoyasadec.cryptotracker.domain.interactor.portfolio.CreatePortfolioCommand
import info.eliumontoyasadec.cryptotracker.domain.interactor.portfolio.CreatePortfolioResult
import info.eliumontoyasadec.cryptotracker.domain.interactor.portfolio.CreatePortfolioUseCase
import info.eliumontoyasadec.cryptotracker.domain.interactor.portfolio.DeletePortfolioCommand
import info.eliumontoyasadec.cryptotracker.domain.interactor.portfolio.DeletePortfolioResult
import info.eliumontoyasadec.cryptotracker.domain.interactor.portfolio.DeletePortfolioUseCase
import info.eliumontoyasadec.cryptotracker.domain.interactor.portfolio.GetAllPortfoliosUseCase
import info.eliumontoyasadec.cryptotracker.domain.interactor.portfolio.SetDefaultPortfolioCommand
import info.eliumontoyasadec.cryptotracker.domain.interactor.portfolio.SetDefaultPortfolioResult
import info.eliumontoyasadec.cryptotracker.domain.interactor.portfolio.SetDefaultPortfolioUseCase
import info.eliumontoyasadec.cryptotracker.domain.interactor.portfolio.UpdatePortfolioCommand
import info.eliumontoyasadec.cryptotracker.domain.interactor.portfolio.UpdatePortfolioResult
import info.eliumontoyasadec.cryptotracker.domain.interactor.portfolio.UpdatePortfolioUseCase
import info.eliumontoyasadec.cryptotracker.domain.model.Portfolio
import kotlinx.coroutines.launch

data class AdminPortfoliosUiState(
    val loading: Boolean = false,
    val items: List<Portfolio> = emptyList(),
    val error: String? = null,

    // Form
    val showForm: Boolean = false,
    val editing: Portfolio? = null,

    // Delete
    val showDeleteConfirm: Boolean = false,
    val pendingDelete: Portfolio? = null
)

class AdminPortfoliosViewModel(
    private val getAll: GetAllPortfoliosUseCase,
    private val createPortfolio: CreatePortfolioUseCase,
    private val updatePortfolio: UpdatePortfolioUseCase,
    private val deletePortfolio: DeletePortfolioUseCase,
    private val setDefaultPortfolio: SetDefaultPortfolioUseCase
) : ViewModel() {

    var state by mutableStateOf(AdminPortfoliosUiState())
        private set

    fun openCreate() {
        state = state.copy(showForm = true, editing = null, error = null)
    }

    fun openEdit(item: Portfolio) {
        state = state.copy(showForm = true, editing = item, error = null)
    }

    fun dismissForm() {
        state = state.copy(showForm = false, editing = null)
    }

    /**
     * Wrapper de UI para mantener la pantalla libre de lógica de orquestación.
     * Decide si es create o update en función de si hay  .
     */
    fun save(name: String, description: String?, makeDefault: Boolean) {
        val editing = state.editing
        if (editing == null) {
            create(name = name, description = description, makeDefault = makeDefault)
        } else {
            update(
                id = editing.portfolioId,
                name = name,
                description = description,
                makeDefault = makeDefault
            )
        }
    }

    fun requestDelete(item: Portfolio) {
        state = state.copy(showDeleteConfirm = true, pendingDelete = item, error = null)
    }

    fun cancelDelete() {
        state = state.copy(showDeleteConfirm = false, pendingDelete = null)
    }

    fun confirmDelete() {
        val target = state.pendingDelete ?: return
        delete(target.portfolioId)
    }

    fun load() {
        viewModelScope.launch {
            state = state.copy(loading = true, error = null)
            try {
                state = state.copy(items = getAll.execute())
            } catch (t: Throwable) {
                state = state.copy(error = t.message ?: "Fallo desconocido")
            } finally {
                state = state.copy(loading = false)
            }
        }
    }

    fun create(
        name: String,
        description: String?,
        makeDefault: Boolean
    ) {
        viewModelScope.launch {
            state = state.copy(loading = true, error = null)

            when (
                val result = createPortfolio.execute(
                    CreatePortfolioCommand(
                        nameRaw = name,
                        descriptionRaw = description,
                        makeDefault = makeDefault
                    )
                )
            ) {
                is CreatePortfolioResult.Success -> {
                    state = state.copy(
                        items = result.items,
                        showForm = false,
                        editing = null
                    )
                }

                is CreatePortfolioResult.ValidationError -> {
                    state = state.copy(error = result.message)
                }

                is CreatePortfolioResult.Failure -> {
                    state = state.copy(error = result.message)
                }
            }

            state = state.copy(loading = false)
        }
    }

    fun update(
        id: Long,
        name: String,
        description: String?,
        makeDefault: Boolean
    ) {
        viewModelScope.launch {
            state = state.copy(loading = true, error = null)

            when (
                val result = updatePortfolio.execute(
                    UpdatePortfolioCommand(
                        id = id,
                        nameRaw = name,
                        descriptionRaw = description,
                        makeDefault = makeDefault
                    )
                )
            ) {
                is UpdatePortfolioResult.Success -> {
                    state = state.copy(
                        items = result.items,
                        showForm = false,
                        editing = null
                    )
                }

                is UpdatePortfolioResult.ValidationError -> {
                    state = state.copy(error = result.message)
                }

                is UpdatePortfolioResult.Failure -> {
                    state = state.copy(error = result.message)
                }
            }

            state = state.copy(loading = false)
        }
    }

    fun delete(id: Long) {
        viewModelScope.launch {
            state = state.copy(loading = true, error = null)

            when (val result = deletePortfolio.execute(DeletePortfolioCommand(id))) {
                is DeletePortfolioResult.Success -> {
                    state = state.copy(
                        items = result.items,
                        showDeleteConfirm = false,
                        pendingDelete = null
                    )
                }

                is DeletePortfolioResult.Failure -> {
                    state = state.copy(
                        items = result.items,
                        showDeleteConfirm = false,
                        pendingDelete = null,
                        error = result.message
                    )
                }
            }

            state = state.copy(loading = false)
        }
    }

    fun setDefault(id: Long) {
        viewModelScope.launch {
            state = state.copy(loading = true, error = null)

            val newState = when (
                val result = setDefaultPortfolio.execute(SetDefaultPortfolioCommand(id))
            ) {
                is SetDefaultPortfolioResult.Success ->
                    state.copy(items = result.items)

                is SetDefaultPortfolioResult.Failure ->
                    state.copy(error = result.message)
            }

            state = newState.copy(loading = false)
        }
    }
}