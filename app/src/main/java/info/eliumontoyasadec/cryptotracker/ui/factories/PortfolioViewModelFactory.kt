package info.eliumontoyasadec.cryptotracker.ui.factories

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import info.eliumontoyasadec.cryptotracker.domain.queries.PortfolioQueries
import info.eliumontoyasadec.cryptotracker.ui.portfolio.PortfolioViewModel

class PortfolioViewModelFactory(
    private val portfolioId: Long,
    private val queries: PortfolioQueries
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(PortfolioViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return PortfolioViewModel(portfolioId, queries) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}