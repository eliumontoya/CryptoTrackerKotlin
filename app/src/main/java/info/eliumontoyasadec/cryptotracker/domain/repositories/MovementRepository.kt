package info.eliumontoyasadec.cryptotracker.domain.repositories

import info.eliumontoyasadec.cryptotracker.domain.model.Movement

interface MovementRepository {
    suspend fun insert(movement: Movement): Long // returns movementId

    suspend fun findById(movementId: Long): Movement?
    suspend fun update(movementId: Long, update: Movement)
    suspend fun delete(movementId: Long)

    suspend fun list(
        portfolioId: Long,
        walletId: Long? = null,
        assetId: String? = null
    ): List<Movement>
}