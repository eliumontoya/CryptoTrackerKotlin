package info.eliumontoyasadec.cryptotracker.room.repositories

import info.eliumontoyasadec.cryptotracker.domain.model.Holding
import info.eliumontoyasadec.cryptotracker.domain.repositories.HoldingRepository
import info.eliumontoyasadec.cryptotracker.room.dao.HoldingDao
import info.eliumontoyasadec.cryptotracker.room.entities.HoldingEntity

class HoldingRepositoryRoom(
    private val dao: HoldingDao
) : HoldingRepository {

    override suspend fun findByWalletAsset(walletId: Long, assetId: String): Holding? {
        return dao.findByWalletAsset(walletId, assetId)?.toDomain()

    }

    // ✅ Implementación alineada para upsert con portfolioId (esto sí está bien en tu interfaz)
    override suspend fun upsert(
        portfolioId: Long,
        walletId: Long,
        assetId: String,
        newQuantity: Double,
        updatedAt: Long
    ): Holding {
        val entity = HoldingEntity(
            id = holdingKey(portfolioId, walletId, assetId),
            portfolioId = portfolioId,
            walletId = walletId,
            assetId = assetId,
            quantity = newQuantity,
            updatedAt = updatedAt
        )
        dao.upsert(entity)
        return entity.toDomain()
    }

}
