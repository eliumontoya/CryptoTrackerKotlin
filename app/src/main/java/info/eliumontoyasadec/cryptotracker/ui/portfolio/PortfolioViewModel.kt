package info.eliumontoyasadec.cryptotracker.ui.portfolio

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import info.eliumontoyasadec.cryptotracker.domain.queries.PortfolioQueries
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

class PortfolioViewModel(
    private val portfolioId: Long,
    private val queries: PortfolioQueries
) : ViewModel() {

    val state: StateFlow<PortfolioUiState> =
        combine(
            queries.portfolioSummary(portfolioId),
            queries.portfolioByCryptos(portfolioId)
        ) { summary, rows ->
            PortfolioUiState(
                title = "Portafolio", // luego lo sacas de PortfolioRepository si quieres
                investedUsd = summary.investedUsd,
                realizedPnlUsd = summary.realizedPnlUsd, // o el campo equivalente en tu SummaryView
                lastUpdatedLabel = summary.updatedAt?.let { "Actualizado: $it" }, // aquí luego formateamos bonito
                rows = rows.map {
                    PortfolioUiState.Row(
                        symbol = it.cryptoSymbol,
                        quantity = it.quantity,
                        costUsd = it.costUsd,
                        realizedPnlUsd = it.realizedPnlUsd
                    )
                }
            )
        }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5_000),
                initialValue = PortfolioUiState(
                    title = "Portafolio",
                    investedUsd = 0.0,
                    realizedPnlUsd = 0.0,
                    rows = emptyList(),
                    lastUpdatedLabel = null
                )
            )
}