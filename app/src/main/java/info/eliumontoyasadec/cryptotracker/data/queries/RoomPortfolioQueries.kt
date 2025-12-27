package info.eliumontoyasadec.cryptotracker.data.queries

import info.eliumontoyasadec.cryptotracker.domain.queries.PortfolioQueries
import info.eliumontoyasadec.cryptotracker.domain.views.PortfolioByCryptoRowView
import info.eliumontoyasadec.cryptotracker.domain.views.PortfolioSummaryView
import info.eliumontoyasadec.cryptotracker.room.queries.PortfolioQueriesDao
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class RoomPortfolioQueries(
    private val dao: PortfolioQueriesDao
) : PortfolioQueries {

    override fun portfolioByCryptos(portfolioId: Long): Flow<List<PortfolioByCryptoRowView>> =
        dao.portfolioByCryptos(portfolioId).map { rows ->
            rows.map { r ->
                PortfolioByCryptoRowView(
                    portfolioId = r.portfolioId,
                    cryptoSymbol = r.cryptoSymbol,
                    quantity = r.quantity,
                    costUsd = r.costUsd,
                    realizedSalesUsd = r.realizedSalesUsd,
                    realizedPnlUsd = r.realizedPnlUsd,
                    currentPriceUsd = null // no prices yet
                )
            }
        }

    override fun portfolioSummary(portfolioId: Long): Flow<PortfolioSummaryView> =
        dao.portfolioSummary(portfolioId).map { r ->
            PortfolioSummaryView(
                portfolioId = r.portfolioId,
                investedUsd = r.investedUsd,
                realizedSalesUsd = r.realizedSalesUsd,
                realizedPnlUsd = r.realizedPnlUsd,
                currentValueUsd = null,   // no prices yet
                totalPnlUsd = null,
                totalPnlPct = null,
                updatedAt = r.updatedAt
            )
        }
}