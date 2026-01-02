package info.eliumontoyasadec.cryptotracker.interactor

import info.eliumontoyasadec.cryptotracker.domain.interactor.portfolio.GetAllPortfoliosUseCase
import info.eliumontoyasadec.cryptotracker.domain.model.Portfolio
import info.eliumontoyasadec.cryptotracker.domain.repositories.PortfolioRepository
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class GetAllPortfoliosUseCaseTest {

    @Test
    fun `execute devuelve lo que regresa el repo`() = runTest {
        val repo = FakePortfolioRepo(
            initial = listOf(
                Portfolio(name = "Main", description = "Principal", isDefault = true),
                Portfolio(name = "Trading", description = null, isDefault = false),
            )
        )
        val uc = GetAllPortfoliosUseCase(repo)

        val result = uc.execute()

        assertEquals(2, result.size)
        assertEquals(1, repo.getAllCalls)
        assertEquals(listOf("Main", "Trading"), result.map { it.name }.sorted())
    }

    // ------------------------------------------------------------
    // Fake PortfolioRepository (100% compatible con tu interface)
    // ------------------------------------------------------------
    private class FakePortfolioRepo(
        initial: List<Portfolio> = emptyList(),
        private val throwOnGetAll: Throwable? = null
    ) : PortfolioRepository {

        private val itemsById = linkedMapOf<Long, Portfolio>()
        private var nextId: Long = 1L
        private var defaultId: Long? = null

        var getAllCalls = 0

        init {
            initial.forEach { p ->
                val id = nextId++
                itemsById[id] = p
                if (p.isDefault) defaultId = id
            }
        }

        override suspend fun exists(portfolioId: Long): Boolean = itemsById.containsKey(portfolioId)

        override suspend fun insert(portfolio: Portfolio): Long {
            val id = nextId++
            itemsById[id] = portfolio
            if (portfolio.isDefault) defaultId = id
            return id
        }

        override suspend fun findById(portfolioId: Long): Portfolio? = itemsById[portfolioId]

        override suspend fun getAll(): List<Portfolio> {
            getAllCalls++
            throwOnGetAll?.let { throw it }
            return itemsById.values.toList()
        }

        override suspend fun getDefault(): Portfolio? =
            defaultId?.let { itemsById[it] }

        override suspend fun update(portfolio: Portfolio) {
            // No-op: este fake no recibe id en Portfolio. Se deja implementado para compatibilidad.
        }

        override suspend fun delete(portfolioId: Long) {
            itemsById.remove(portfolioId)
            if (defaultId == portfolioId) defaultId = null
        }

        override suspend fun delete(portfolio: Portfolio) {
            val entry = itemsById.entries.firstOrNull { it.value == portfolio } ?: return
            delete(entry.key)
        }

        override suspend fun isDefault(portfolioId: Long): Boolean = defaultId == portfolioId

        override suspend fun setDefault(portfolioId: Long) {
            if (!itemsById.containsKey(portfolioId)) return
            defaultId = portfolioId
            // No reescribimos isDefault dentro de Portfolio porque no tenemos id dentro del modelo.
        }
    }
}