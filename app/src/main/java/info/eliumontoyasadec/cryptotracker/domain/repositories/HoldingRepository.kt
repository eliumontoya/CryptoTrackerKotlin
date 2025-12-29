package info.eliumontoyasadec.cryptotracker.domain.repositories

import info.eliumontoyasadec.cryptotracker.domain.model.Holding

interface HoldingRepository {
    suspend fun findByWalletAsset(walletId: Long, assetId: String): Holding?
    suspend fun upsert(portfolioId: Long, walletId: Long, assetId: String, newQuantity: Double, updatedAt: Long): Holding
}