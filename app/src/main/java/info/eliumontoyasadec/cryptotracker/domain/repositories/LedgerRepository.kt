package info.eliumontoyasadec.cryptotracker.domain.repositories

import androidx.room.withTransaction
import info.eliumontoyasadec.cryptotracker.room.db.AppDatabase
import info.eliumontoyasadec.cryptotracker.room.entities.HoldingEntity
import info.eliumontoyasadec.cryptotracker.room.entities.MovementEntity
import info.eliumontoyasadec.cryptotracker.domain.model.MovementType

class LedgerRepository(
    private val db: AppDatabase
) {
    private val txDao = db.movementDao()
    private val holdingDao = db.holdingDao()

    suspend fun addTransaction(tx: MovementEntity) {
        val now = System.currentTimeMillis()
/*
        db.withTransaction {
            txDao.insert(tx)

            val delta = tx.toHoldingDelta()

            val existing = holdingDao.find(tx.walletId, tx.cryptoSymbol)
            if (existing == null) {
                holdingDao.insert(
                    HoldingEntity(
                        walletId = tx.walletId,
                        cryptoSymbol = tx.cryptoSymbol,
                        quantity = delta,
                        updatedAt = now
                    )
                )
            } else {
                holdingDao.applyDelta(
                    holdingId = existing.holdingId,
                    delta = delta,
                    updatedAt = now
                )
            }
        }
        */

    }

    private fun MovementEntity.toHoldingDelta(): Double {
        return when (type) {
            MovementType.BUY,
            MovementType.DEPOSIT,
            MovementType.TRANSFER_IN,
            MovementType.ADJUSTMENT -> quantity

            MovementType.SELL,
            MovementType.WITHDRAW,
            MovementType.TRANSFER_OUT,
            MovementType.FEE -> -quantity
        }
    }
}