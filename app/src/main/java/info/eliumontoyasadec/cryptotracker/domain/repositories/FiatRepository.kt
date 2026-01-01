package info.eliumontoyasadec.cryptotracker.domain.repositories

import info.eliumontoyasadec.cryptotracker.domain.model.Fiat

interface FiatRepository {
    suspend fun exists(code: String): Boolean

    suspend fun upsertAll(items: List<Fiat>)
    suspend fun getAll(): List<Fiat>
    suspend fun findByCode(code: String): Fiat?
    suspend fun countAll(): Int

    suspend fun upsert(item: Fiat)
    suspend fun delete(code: String): Boolean
}