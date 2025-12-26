package info.eliumontoyasadec.cryptotracker.domain.repositories

interface WalletRepository {
    suspend fun exists(walletId: String): Boolean
    suspend fun belongsToPortfolio(walletId: String, portfolioId: String): Boolean
}