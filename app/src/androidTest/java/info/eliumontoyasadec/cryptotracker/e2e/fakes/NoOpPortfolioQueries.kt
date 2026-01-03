package info.eliumontoyasadec.cryptotracker.e2e.fakes

import info.eliumontoyasadec.cryptotracker.domain.queries.PortfolioQueries
import info.eliumontoyasadec.cryptotracker.domain.views.PortfolioByCryptoRowView
import info.eliumontoyasadec.cryptotracker.domain.views.PortfolioSummaryView
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow

class NoOpPortfolioQueries : PortfolioQueries {
    override fun portfolioByCryptos(portfolioId: Long): Flow<List<PortfolioByCryptoRowView>> = emptyFlow()
    override fun portfolioSummary(portfolioId: Long): Flow<PortfolioSummaryView> = emptyFlow()
}