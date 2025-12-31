package info.eliumontoyasadec.cryptotracker.room.repositories

import info.eliumontoyasadec.cryptotracker.domain.model.Portfolio
import info.eliumontoyasadec.cryptotracker.domain.repositories.PortfolioRepository
import info.eliumontoyasadec.cryptotracker.room.dao.PortfolioDao
import info.eliumontoyasadec.cryptotracker.room.entities.PortfolioEntity

class PortfolioRepositoryRoom(
    private val dao: PortfolioDao
) : PortfolioRepository {

    override suspend fun exists(portfolioId: Long): Boolean =
        dao.exists(portfolioId)



    override suspend fun findById(portfolioId: Long): Portfolio? =
        dao.getById(portfolioId)?.toDomain()

    override suspend fun getAll(): List<Portfolio> =
        dao.getAll().map { it.toDomain() }

    override suspend fun getDefault(): Portfolio? =
        dao.getDefault()?.toDomain()

    override suspend fun insert(portfolio: Portfolio): Long {
        val normalized = portfolio.copy(
            name = portfolio.name.trim(),
            description = portfolio.description?.trim().takeUnless { it.isNullOrBlank() }
        )

        if (normalized.isDefault) {
            dao.clearDefault()
        }
        return dao.insert(normalized.toEntity())
    }

    override suspend fun update(portfolio: Portfolio) {
        val current = dao.getById(portfolio.portfolioId) ?: throw IllegalArgumentException("Portfolio not found: ${portfolio.portfolioId}")
        val normalized = portfolio.copy(
            name = portfolio.name.trim(),
            description = portfolio.description?.trim().takeUnless { it.isNullOrBlank() }
        )

        if (normalized.isDefault && !current.isDefault) {
            dao.clearDefault()
        }

        dao.update(normalized.toEntity())
    }

    override suspend fun delete(portfolioId: Long) {
        val existing = dao.getById(portfolioId) ?: return
        dao.delete(existing)
    }

    override suspend fun delete(portfolio: Portfolio) {
        val existing = dao.getById(portfolio.portfolioId) ?: return
         dao.delete(existing)
    }

    override suspend fun isDefault(portfolioId: Long): Boolean {
        return dao.isDefault(portfolioId)
    }

    override suspend fun setDefault(portfolioId: Long) {
        val current = dao.getById(portfolioId)
            ?: error("Portfolio $portfolioId no existe")

        if (current.isDefault) return // idempotente

        dao.clearDefault()
        dao.update(current.copy(isDefault = true))
    }
}

/* =======================
   MAPPERS (igual patrón que Wallet)
   ======================= */

fun PortfolioEntity.toDomain(): Portfolio = Portfolio(
    portfolioId = portfolioId,
    name = name,
    description = description,
    isDefault = isDefault
)

fun Portfolio.toEntity(): PortfolioEntity = PortfolioEntity(
    portfolioId = portfolioId,
    name = name,
    description = description,
    isDefault = isDefault
)