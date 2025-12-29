package info.eliumontoyasadec.cryptotracker.room.repositories

import androidx.room.withTransaction
import info.eliumontoyasadec.cryptotracker.domain.repositories.TransactionRunner
import info.eliumontoyasadec.cryptotracker.room.db.AppDatabase

class TransactionRunnerRoom(
    private val db: AppDatabase
) : TransactionRunner {

    override suspend fun <T> runInTransaction(block: suspend () -> T): T {
        return db.withTransaction { block() }
    }
}