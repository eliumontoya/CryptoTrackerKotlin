package info.eliumontoyasadec.cryptotracker.domain.repositories

import info.eliumontoyasadec.cryptotracker.domain.model.Holding

interface HoldingRepository {
    suspend fun findByWalletAsset(walletId: String, assetId: String): Holding?
    suspend fun upsert(portfolioId: String, walletId: String, assetId: String, newQuantity: Double, updatedAt: Long): Holding
}