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

private fun HoldingEntity.toDomain(): Holding = Holding(
    id = id,
    portfolioId = portfolioId,
    walletId = walletId,
    assetId = assetId,
    quantity = quantity,
    updatedAt = updatedAt
)

private fun holdingKey(portfolioId: String, walletId: String, assetId: String): String =
    "$portfolioId|$walletId|$assetId"