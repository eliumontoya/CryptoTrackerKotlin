package info.eliumontoyasadec.cryptotracker.room.repositories

import androidx.room.withTransaction
import info.eliumontoyasadec.cryptotracker.room.db.AppDatabase
import info.eliumontoyasadec.cryptotracker.room.entities.HoldingEntity
import info.eliumontoyasadec.cryptotracker.room.entities.TransactionEntity
import info.eliumontoyasadec.cryptotracker.room.entities.TransactionType

class LedgerRepository(
    private val db: AppDatabase
) {
    private val txDao = db.transactionDao()
    private val holdingDao = db.holdingDao()

    suspend fun addTransaction(tx: TransactionEntity) {
        val now = System.currentTimeMillis()

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
    }

    private fun TransactionEntity.toHoldingDelta(): Double {
        return when (type) {
            TransactionType.BUY,
            TransactionType.TRANSFER_IN,
            TransactionType.ADJUSTMENT -> quantity

            TransactionType.SELL,
            TransactionType.TRANSFER_OUT -> -quantity
        }
    }
}