package info.eliumontoyasadec.cryptotracker.domain.repositories
interface PortfolioRepository {
    suspend fun exists(portfolioId: String): Boolean
}