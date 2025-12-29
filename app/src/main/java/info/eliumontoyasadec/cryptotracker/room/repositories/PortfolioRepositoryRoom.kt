package info.eliumontoyasadec.cryptotracker.room.repositories

import info.eliumontoyasadec.cryptotracker.domain.repositories.PortfolioRepository
import info.eliumontoyasadec.cryptotracker.room.dao.PortfolioDao
import info.eliumontoyasadec.cryptotracker.room.entities.PortfolioEntity

class PortfolioRepositoryRoom(
    private val dao: PortfolioDao
) : PortfolioRepository {

    override suspend fun exists(portfolioId: Long): Boolean =
        dao.getById(portfolioId) != null

    suspend fun getAll(): List<PortfolioEntity> = dao.getAll()

    suspend fun getById(portfolioId: Long): PortfolioEntity? = dao.getById(portfolioId)

    suspend fun getDefault(): PortfolioEntity? = dao.getDefault()

    suspend fun insert(entity: PortfolioEntity): Long = dao.insert(entity)

    suspend fun update(entity: PortfolioEntity) = dao.update(entity)

    suspend fun delete(entity: PortfolioEntity) = dao.delete(entity)
}