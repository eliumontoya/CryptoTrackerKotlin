package info.eliumontoyasadec.cryptotracker.room.repositories

import androidx.room.withTransaction
import info.eliumontoyasadec.cryptotracker.domain.model.Holding
import info.eliumontoyasadec.cryptotracker.domain.model.Movement
import info.eliumontoyasadec.cryptotracker.domain.repositories.MovementRepository
import info.eliumontoyasadec.cryptotracker.domain.repositories.TransactionRunner
import info.eliumontoyasadec.cryptotracker.room.dao.MovementDao
import info.eliumontoyasadec.cryptotracker.room.db.AppDatabase
import info.eliumontoyasadec.cryptotracker.room.entities.HoldingEntity
import info.eliumontoyasadec.cryptotracker.room.entities.MovementEntity

class MovementRepositoryRoom(
    private val dao: MovementDao
) : MovementRepository {

    override suspend fun insert(movement: Movement): Long {
        // Si el id es 0, Room lo genera
        val entity = movement.toEntity(
            id = if (movement.id > 0) movement.id else 0L
        )
        dao.upsert(entity)
        return entity.id
    }


    override suspend fun findById(movementId: Long): Movement? {
        return dao.findById(movementId)?.toDomain()
    }

    override suspend fun update(movementId: Long, update: Movement) {
        val old = dao.findById(movementId) ?: return

        // Mantengo ids/base. Solo actualizo el contenido editable.
        val merged = old.copy(
            type = update.type,
            quantity = update.quantity,
            price = update.price,
            feeQuantity = update.feeQuantity,
            timestamp = update.timestamp,
            notes = update.notes,
            groupId = update.groupId ?: old.groupId
        )

        dao.update(merged)
    }

    override suspend fun delete(movementId: Long) {
        dao.deleteById(movementId)
    }
}



/* =======================
   MAPPERS
   ======================= */

private fun Movement.toEntity(id: Long): MovementEntity = MovementEntity(
    id = id,
    portfolioId = portfolioId,
    walletId = walletId,
    assetId = assetId,
    type = type,
    quantity = quantity,
    price = price,
    feeQuantity = feeQuantity,
    timestamp = timestamp,
    notes = notes,
    groupId = groupId
)

private fun MovementEntity.toDomain(): Movement = Movement(
    id = id,
    portfolioId = portfolioId,
    walletId = walletId,
    assetId = assetId,
    type = type,
    quantity = quantity,
    price = price,
    feeQuantity = feeQuantity,
    timestamp = timestamp,
    notes = notes,
    groupId = groupId
)
