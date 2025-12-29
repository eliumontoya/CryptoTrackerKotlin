package info.eliumontoyasadec.cryptotracker.room.repositories

import info.eliumontoyasadec.cryptotracker.domain.repositories.WalletRepository
import info.eliumontoyasadec.cryptotracker.room.dao.WalletDao
import info.eliumontoyasadec.cryptotracker.room.entities.WalletEntity

class WalletRepositoryRoom(
    private val dao: WalletDao
) : WalletRepository {

    override suspend fun exists(walletId: Long): Boolean =
        dao.getById(walletId) != null

    override suspend fun belongsToPortfolio(walletId: Long, portfolioId: Long): Boolean =
        dao.getById(walletId)?.portfolioId == portfolioId

    suspend fun getByPortfolio(portfolioId: Long): List<WalletEntity> = dao.getByPortfolio(portfolioId)

    suspend fun getById(walletId: Long): WalletEntity? = dao.getById(walletId)

    suspend fun insert(entity: WalletEntity): Long = dao.insert(entity)

    suspend fun update(entity: WalletEntity) = dao.update(entity)

    suspend fun delete(entity: WalletEntity) = dao.delete(entity)
}