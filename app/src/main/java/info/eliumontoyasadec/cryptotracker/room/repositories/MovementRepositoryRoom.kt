package info.eliumontoyasadec.cryptotracker.room.repositories

import androidx.room.withTransaction
import info.eliumontoyasadec.cryptotracker.domain.model.Holding
import info.eliumontoyasadec.cryptotracker.domain.model.Movement
import info.eliumontoyasadec.cryptotracker.domain.repositories.HoldingRepository
import info.eliumontoyasadec.cryptotracker.domain.repositories.MovementRepository
import info.eliumontoyasadec.cryptotracker.domain.repositories.TransactionRunner
import info.eliumontoyasadec.cryptotracker.room.dao.HoldingDao
import info.eliumontoyasadec.cryptotracker.room.dao.MovementDao
import info.eliumontoyasadec.cryptotracker.room.db.AppDatabase
import info.eliumontoyasadec.cryptotracker.room.entities.HoldingEntity
import info.eliumontoyasadec.cryptotracker.room.entities.MovementEntity
import java.util.UUID

class MovementRepositoryRoom(
    private val dao: MovementDao
) : MovementRepository {

    override suspend fun insert(movement: Movement): String {
        val id = movement.id.takeIf { it.isNotBlank() } ?: UUID.randomUUID().toString()
        dao.upsert(movement.toEntity(id))
        return id
    }

    override suspend fun findById(movementId: String): Movement? {
        return dao.findById(movementId)?.toDomain()
    }

    override suspend fun update(movementId: String, update: Movement) {
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

    override suspend fun delete(movementId: String) {
        dao.deleteById(movementId)
    }
}

class TransactionRunnerRoom(
    private val db: AppDatabase
) : TransactionRunner {

    override suspend fun <T> runInTransaction(block: suspend () -> T): T {
        return db.withTransaction { block() }
    }
}

/* =======================
   MAPPERS
   ======================= */

private fun Movement.toEntity(id: String): MovementEntity = MovementEntity(
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

fun HoldingEntity.toDomain(): Holding = Holding(
    id = id,
    portfolioId = portfolioId,
    walletId = walletId,
    assetId = assetId,
    quantity = quantity,
    updatedAt = updatedAt
)

fun holdingKey(portfolioId: String, walletId: String, assetId: String): String =
    "$portfolioId|$walletId|$assetId"