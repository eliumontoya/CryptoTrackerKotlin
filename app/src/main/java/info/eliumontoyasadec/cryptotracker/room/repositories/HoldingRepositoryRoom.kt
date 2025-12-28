package info.eliumontoyasadec.cryptotracker.room.repositories

import info.eliumontoyasadec.cryptotracker.domain.model.Holding
import info.eliumontoyasadec.cryptotracker.domain.repositories.HoldingRepository
import info.eliumontoyasadec.cryptotracker.room.dao.HoldingDao
import info.eliumontoyasadec.cryptotracker.room.entities.HoldingEntity

class HoldingRepositoryRoom(
    private val dao: HoldingDao
) : HoldingRepository {

    override suspend fun findByWalletAsset(walletId: String, assetId: String): Holding? {
        // ⚠️ Tu interfaz actual no recibe portfolioId aquí.
        // Por diseño correcto, debería recibirlo.
        // Mientras tanto: asumimos que walletId/assetId ya están en un portfolio único.
        // RECOMENDACIÓN: ajustar interfaz a (portfolioId, walletId, assetId).
        throw IllegalStateException(
            "HoldingRepository.findByWalletAsset(walletId, assetId) no es suficiente sin portfolioId. " +
                    "Ajusta la interfaz a findByPortfolioWalletAsset(portfolioId, walletId, assetId)."
        )
    }

    // ✅ Implementación alineada para upsert con portfolioId (esto sí está bien en tu interfaz)
    override suspend fun upsert(
        portfolioId: String,
        walletId: String,
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
