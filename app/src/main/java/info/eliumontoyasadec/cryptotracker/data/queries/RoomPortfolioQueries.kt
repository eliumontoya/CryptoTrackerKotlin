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

    override fun portfolioByCryptos(portfolioId: Long): Flow<List<PortfolioByCryptoRowView>> {
        return dao.portfolioByCryptos(portfolioId).map { rows ->
            rows.map { r ->
                PortfolioByCryptoRowView(
                    portfolioId = r.portfolioId,
                    cryptoSymbol = r.assetId,          // mantenemos símbolo para UI
                    quantity = r.quantity,
                    costUsd = r.costUsd,               // Camino 1: 0.0 desde SQL
                    realizedSalesUsd = r.realizedUsd,  // Camino 1: 0.0 desde SQL
                    realizedPnlUsd = r.pnlUsd,         // Camino 1: 0.0 desde SQL
                    currentPriceUsd = null             // aún no hay prices (Camino 2)
                )
            }
        }
    }

    override fun portfolioSummary(portfolioId: Long): Flow<PortfolioSummaryView> {
        return dao.portfolioSummary(portfolioId).map { r ->
            PortfolioSummaryView(
                portfolioId = r.portfolioId,
                investedUsd = r.investedUsd,          // Camino 1: 0.0 desde SQL
                realizedSalesUsd = r.realizedUsd,     // Camino 1: 0.0 desde SQL
                realizedPnlUsd = 0.0,                 // Camino 1: aún no hay ledger
                currentValueUsd = null,               // aún no hay prices
                totalPnlUsd = null,                   // se habilita con prices/ledger
                totalPnlPct = null,                   // se habilita con prices/ledger
                updatedAt = r.updatedAt               // esto sí es real (MAX(updatedAt))
            )
        }
    }
}