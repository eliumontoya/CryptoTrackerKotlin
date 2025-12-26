package info.eliumontoyasadec.cryptotracker.domain.repositories
interface TransactionRunner {
    suspend fun <T> runInTransaction(block: suspend () -> T): T
}