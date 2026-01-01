package info.eliumontoyasadec.cryptotracker.domain.repositories

import info.eliumontoyasadec.cryptotracker.domain.model.Wallet

interface WalletRepository {
    suspend fun exists(walletId: Long): Boolean
    suspend fun belongsToPortfolio(walletId: Long, portfolioId: Long): Boolean

    suspend fun insert(wallet: Wallet): Long
    suspend fun findById(walletId: Long): Wallet?
    suspend fun getByPortfolio(portfolioId: Long): List<Wallet>
    suspend fun update(wallet: Wallet)
    suspend fun delete(walletId: Long)
    // Default (main)
    suspend fun isMain(walletId: Long): Boolean
    suspend fun setMain(walletId: Long)
    suspend fun update(walletId: Long, name: String)
}