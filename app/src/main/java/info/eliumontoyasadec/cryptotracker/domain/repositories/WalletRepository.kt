package info.eliumontoyasadec.cryptotracker.domain.repositories

interface WalletRepository {
    suspend fun exists(walletId: Long): Boolean
    suspend fun belongsToPortfolio(walletId: Long, portfolioId: Long): Boolean
}