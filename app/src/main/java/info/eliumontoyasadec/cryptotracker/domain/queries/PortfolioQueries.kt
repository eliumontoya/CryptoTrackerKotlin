package info.eliumontoyasadec.cryptotracker.domain.queries

import info.eliumontoyasadec.cryptotracker.domain.views.PortfolioByCryptoRowView
import info.eliumontoyasadec.cryptotracker.domain.views.PortfolioSummaryView
import kotlinx.coroutines.flow.Flow

interface PortfolioQueries {

    /** Tabla “Portafolio por Cryptos” (sin prices por ahora). */
    fun portfolioByCryptos(portfolioId: Long): Flow<List<PortfolioByCryptoRowView>>

    /** Cards de resumen (invertido, ventas, pnl realizado; y cuando haya prices, valor actual). */
    fun portfolioSummary(portfolioId: Long): Flow<PortfolioSummaryView>
}