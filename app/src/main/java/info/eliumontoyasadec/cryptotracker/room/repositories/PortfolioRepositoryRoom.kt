package info.eliumontoyasadec.cryptotracker.room.repositories

import info.eliumontoyasadec.cryptotracker.domain.model.Portfolio
import info.eliumontoyasadec.cryptotracker.domain.repositories.PortfolioRepository
import info.eliumontoyasadec.cryptotracker.room.dao.PortfolioDao
import info.eliumontoyasadec.cryptotracker.room.entities.PortfolioEntity

class PortfolioRepositoryRoom(
    private val dao: PortfolioDao
) : PortfolioRepository {

    override suspend fun exists(portfolioId: Long): Boolean =
        dao.getById(portfolioId) != null

    override suspend fun insert(portfolio: Portfolio): Long =
        dao.insert(portfolio.toEntity())

    override suspend fun findById(portfolioId: Long): Portfolio? =
        dao.getById(portfolioId)?.toDomain()

    override suspend fun getAll(): List<Portfolio> =
        dao.getAll().map { it.toDomain() }

    override suspend fun getDefault(): Portfolio? =
        dao.getDefault()?.toDomain()

    override suspend fun update(portfolio: Portfolio) {
        val existing = dao.getById(portfolio.portfolioId) ?: return
        dao.update(
            existing.copy(
                name = portfolio.name,
                description = portfolio.description,
                isDefault = portfolio.isDefault
            )
        )
    }

    override suspend fun delete(portfolioId: Long) {
        val existing = dao.getById(portfolioId) ?: return
        dao.delete(existing)
    }
}

/* =======================
   MAPPERS (igual patrón que Wallet)
   ======================= */

private fun PortfolioEntity.toDomain(): Portfolio = Portfolio(
    portfolioId = portfolioId,
    name = name,
    description = description,
    isDefault = isDefault
)

private fun Portfolio.toEntity(): PortfolioEntity = PortfolioEntity(
    portfolioId = portfolioId,
    name = name,
    description = description,
    isDefault = isDefault
)