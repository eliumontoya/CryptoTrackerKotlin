package info.eliumontoyasadec.cryptotracker.room.repositories

import info.eliumontoyasadec.cryptotracker.domain.model.Crypto
import info.eliumontoyasadec.cryptotracker.domain.repositories.CryptoRepository
import info.eliumontoyasadec.cryptotracker.room.dao.CryptoDao
import info.eliumontoyasadec.cryptotracker.room.entities.CryptoEntity

class CryptoRepositoryRoom(
    private val dao: CryptoDao
) : CryptoRepository {

    override suspend fun exists(assetId: String): Boolean =
        dao.getBySymbol(assetId) != null

    override suspend fun upsertAll(items: List<Crypto>) =
        dao.upsertAll(items.map { it.toEntity() })

    override suspend fun getAll(): List<Crypto> =
        dao.getAll().map { it.toDomain() }

    override suspend fun findBySymbol(symbol: String): Crypto? =
        dao.getBySymbol(symbol)?.toDomain()
}

/* =======================
   MAPPERS
   ======================= */

private fun CryptoEntity.toDomain(): Crypto = Crypto(
    symbol = symbol,
    name = name,
    coingeckoId = coingeckoId,
    isActive = isActive
)

private fun Crypto.toEntity(): CryptoEntity = CryptoEntity(
    symbol = symbol,
    name = name,
    coingeckoId = coingeckoId,
    isActive = isActive
)