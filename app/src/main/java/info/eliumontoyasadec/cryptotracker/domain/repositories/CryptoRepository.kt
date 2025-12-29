package info.eliumontoyasadec.cryptotracker.domain.repositories

import info.eliumontoyasadec.cryptotracker.domain.model.Crypto

interface CryptoRepository {
    suspend fun exists(assetId: String): Boolean

    suspend fun upsertAll(items: List<Crypto>)
    suspend fun getAll(): List<Crypto>
    suspend fun findBySymbol(symbol: String): Crypto?
}