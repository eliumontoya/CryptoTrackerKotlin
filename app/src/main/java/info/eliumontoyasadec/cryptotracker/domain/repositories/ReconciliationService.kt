package info.eliumontoyasadec.cryptotracker.room.services

import androidx.room.withTransaction
import info.eliumontoyasadec.cryptotracker.room.db.AppDatabase
import info.eliumontoyasadec.cryptotracker.room.entities.HoldingEntity

class ReconciliationService(
    private val db: AppDatabase
) {
    private val txDao = db.transactionDao()
    private val holdingDao = db.holdingDao()

    suspend fun rebuildAllHoldings() {
        val now = System.currentTimeMillis()
/*
        db.withTransaction {
            holdingDao.deleteAll()
            val rows = txDao.computeBalancesAll()
            rows.forEach { r ->
                holdingDao.insert(
                    HoldingEntity(
                        walletId = r.walletId,
                        cryptoSymbol = r.cryptoSymbol,
                        quantity = r.balance,
                        updatedAt = now
                    )
                )
            }
        }

 */
    }

    suspend fun rebuildHoldingsForWallet(walletId: Long) {
        val now = System.currentTimeMillis()
/*
        db.withTransaction {
            holdingDao.deleteByWallet(walletId)
            val rows = txDao.computeBalancesByWallet(walletId)
            rows.forEach { r ->
                holdingDao.insert(
                    HoldingEntity(
                        walletId = r.walletId,
                        cryptoSymbol = r.cryptoSymbol,
                        quantity = r.balance,
                        updatedAt = now
                    )
                )
            }
        }
    }

 */
    }}