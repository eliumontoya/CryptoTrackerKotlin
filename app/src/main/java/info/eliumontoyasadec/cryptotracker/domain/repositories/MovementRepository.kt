package info.eliumontoyasadec.cryptotracker.domain.repositories

import info.eliumontoyasadec.cryptotracker.domain.model.Movement

interface MovementRepository {
    suspend fun insert(movement: Movement): String // returns movementId

    suspend fun findById(movementId: String): Movement?
    suspend fun update(movementId: String, update: Movement)
    suspend fun delete(movementId: String)

}