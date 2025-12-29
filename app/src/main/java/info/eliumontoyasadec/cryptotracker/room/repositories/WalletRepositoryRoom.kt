// File: room/repositories/WalletRoomRepository.kt
package info.eliumontoyasadec.cryptotracker.room.repositories

import info.eliumontoyasadec.cryptotracker.room.dao.WalletDao
import info.eliumontoyasadec.cryptotracker.room.entities.WalletEntity

class WalletRepositoryRoom(
    private val dao: WalletDao
) {
    suspend fun getByPortfolio(portfolioId: Long): List<WalletEntity> = dao.getByPortfolio(portfolioId)

    suspend fun getById(walletId: Long): WalletEntity? = dao.getById(walletId)

    suspend fun insert(entity: WalletEntity): Long = dao.insert(entity)

    suspend fun update(entity: WalletEntity) = dao.update(entity)

    suspend fun delete(entity: WalletEntity) = dao.delete(entity)
}