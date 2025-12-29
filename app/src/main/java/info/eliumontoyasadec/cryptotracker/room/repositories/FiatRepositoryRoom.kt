package info.eliumontoyasadec.cryptotracker.room.repositories

import info.eliumontoyasadec.cryptotracker.room.dao.FiatDao
import info.eliumontoyasadec.cryptotracker.room.entities.FiatEntity

class FiatRepositoryRoom(
    private val dao: FiatDao
) {
    suspend fun getAll(): List<FiatEntity> = dao.getAll()

    suspend fun getByCode(code: String): FiatEntity? = dao.getByCode(code)

    suspend fun upsertAll(items: List<FiatEntity>) = dao.upsertAll(items)
}