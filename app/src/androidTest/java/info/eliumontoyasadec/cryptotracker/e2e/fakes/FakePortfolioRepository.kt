package info.eliumontoyasadec.cryptotracker.e2e.fakes

import info.eliumontoyasadec.cryptotracker.domain.model.Portfolio
import info.eliumontoyasadec.cryptotracker.domain.repositories.PortfolioRepository

class FakePortfolioRepository : PortfolioRepository {

    private val items = mutableListOf<Portfolio>()
    private var nextId = 1L

    override suspend fun exists(portfolioId: Long): Boolean =
        items.any { it.portfolioId == portfolioId }

    override suspend fun insert(portfolio: Portfolio): Long {
        val id = nextId++
        val toInsert = portfolio.copy(portfolioId = id)

        items.add(toInsert)

        // Regla: si se inserta como default, solo uno puede quedar default
        if (toInsert.isDefault) {
            setDefault(id)
        } else {
            // Si no hay default aún, podrías decidir dejarlo sin default (como hoy lo hace tu lógica).
            // No forzamos nada aquí.
        }

        return id
    }

    override suspend fun findById(portfolioId: Long): Portfolio? =
        items.firstOrNull { it.portfolioId == portfolioId }

    override suspend fun getAll(): List<Portfolio> =
        items.sortedBy { it.portfolioId }

    override suspend fun getDefault(): Portfolio? =
        items.firstOrNull { it.isDefault }

    override suspend fun update(portfolio: Portfolio) {
        val idx = items.indexOfFirst { it.portfolioId == portfolio.portfolioId }
        if (idx < 0) throw IllegalArgumentException("Portfolio not found: ${portfolio.portfolioId}")

        items[idx] = portfolio

        if (portfolio.isDefault) {
            setDefault(portfolio.portfolioId)
        }
    }

    override suspend fun delete(portfolioId: Long) {
        items.removeAll { it.portfolioId == portfolioId }
    }

    override suspend fun delete(portfolio: Portfolio) {
        delete(portfolio.portfolioId)
    }

    override suspend fun isDefault(portfolioId: Long): Boolean =
        items.firstOrNull { it.portfolioId == portfolioId }?.isDefault == true

    override suspend fun setDefault(portfolioId: Long) {
        if (!exists(portfolioId)) throw IllegalArgumentException("Portfolio not found: $portfolioId")

        for (i in items.indices) {
            val p = items[i]
            items[i] = p.copy(isDefault = p.portfolioId == portfolioId)
        }
    }
}