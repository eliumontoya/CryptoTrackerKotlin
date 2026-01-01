package info.eliumontoyasadec.cryptotracker.room.repositories

import info.eliumontoyasadec.cryptotracker.domain.model.Fiat
import info.eliumontoyasadec.cryptotracker.domain.repositories.FiatRepository
import info.eliumontoyasadec.cryptotracker.room.dao.FiatDao
import info.eliumontoyasadec.cryptotracker.room.entities.FiatEntity

class FiatRepositoryRoom(
    private val dao: FiatDao
) : FiatRepository {

    override suspend fun exists(code: String): Boolean =
        dao.getByCode(code) != null

    override suspend fun upsertAll(items: List<Fiat>) =
        dao.upsertAll(items.map { it.toEntity() })

    override suspend fun getAll(): List<Fiat> =
        dao.getAll().map { it.toDomain() }

    override suspend fun findByCode(code: String): Fiat? =
        dao.getByCode(code)?.toDomain()

    override suspend fun upsert(item: Fiat) {
        dao.upsert(item.toEntity())
    }

    override suspend fun countAll(): Int =
        dao.countAll()
    override suspend fun delete(code: String): Boolean {
        return try {
            dao.deleteByCode(code) > 0
        } catch (t: Throwable) {
             throw t
        }
    }

}

/* =======================
   MAPPERS
   ======================= */

private fun FiatEntity.toDomain(): Fiat = Fiat(
    code = code,
    name = name,
    symbol = symbol
)

private fun Fiat.toEntity(): FiatEntity = FiatEntity(
    code = code,
    name = name,
    symbol = symbol
)