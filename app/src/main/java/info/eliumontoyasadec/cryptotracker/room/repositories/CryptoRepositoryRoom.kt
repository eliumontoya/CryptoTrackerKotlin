// File: room/repositories/CryptoRoomRepository.kt
package info.eliumontoyasadec.cryptotracker.room.repositories

import info.eliumontoyasadec.cryptotracker.room.dao.CryptoDao
import info.eliumontoyasadec.cryptotracker.room.entities.CryptoEntity

class CryptoRepositoryRoom(
    private val dao: CryptoDao
) {
    suspend fun getAll(): List<CryptoEntity> = dao.getAll()

    suspend fun getBySymbol(symbol: String): CryptoEntity? = dao.getBySymbol(symbol)

    suspend fun upsertAll(items: List<CryptoEntity>) = dao.upsertAll(items)
}