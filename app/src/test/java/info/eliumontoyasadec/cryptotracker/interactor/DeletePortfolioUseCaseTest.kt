package info.eliumontoyasadec.cryptotracker.interactor

import info.eliumontoyasadec.cryptotracker.domain.interactor.portfolio.DeletePortfolioCommand
import info.eliumontoyasadec.cryptotracker.domain.interactor.portfolio.DeletePortfolioResult
import info.eliumontoyasadec.cryptotracker.domain.interactor.portfolio.DeletePortfolioUseCase
import info.eliumontoyasadec.cryptotracker.domain.model.Portfolio
import info.eliumontoyasadec.cryptotracker.domain.repositories.PortfolioRepository
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Test

class DeletePortfolioUseCaseTest {

    @Test
    fun `failure - id invalido regresa safeGetAll`() = runTest {
        val repo = FakePortfolioRepo(
            initial = listOf(
                Portfolio(name = "Main", description = null, isDefault = true)
            )
        )
        val uc = DeletePortfolioUseCase(repo)

        val result = uc.execute(DeletePortfolioCommand(id = 0))

        assertTrue(result is DeletePortfolioResult.Failure)
        val f = result as DeletePortfolioResult.Failure
        assertEquals("Id inválido.", f.message)
        assertEquals(listOf("Main"), f.items.map { it.name })
        assertEquals(0, repo.deleteCalls)
        assertEquals(1, repo.getAllCalls) // safeGetAll
    }

    @Test
    fun `success - delete ejecuta y devuelve getAll`() = runTest {
        val repo = FakePortfolioRepo(
            initial = listOf(
                Portfolio(name = "Main", description = null, isDefault = true),
                Portfolio(name = "Trading", description = null, isDefault = false)
            )
        )
        val uc = DeletePortfolioUseCase(repo)

        val result = uc.execute(DeletePortfolioCommand(id = 1))

        assertTrue(result is DeletePortfolioResult.Success)
        val s = result as DeletePortfolioResult.Success

        assertEquals(1, repo.deleteCalls)
        assertEquals(1L, repo.lastDeletedId)
        assertEquals(1, repo.getAllCalls)

        // Como el fake no “mapea” id dentro del modelo, la eliminación es por id interno del fake:
        // id=1 borra el primer insertado (Main).
        assertEquals(listOf("Trading"), s.items.map { it.name })
    }

    @Test
    fun `failure - si delete lanza excepcion regresa safeGetAll`() = runTest {
        val repo = FakePortfolioRepo(
            initial = listOf(
                Portfolio(name = "Main", description = null, isDefault = true)
            ),
            throwOnDelete = IllegalStateException("FK constraint")
        )
        val uc = DeletePortfolioUseCase(repo)

        val result = uc.execute(DeletePortfolioCommand(id = 1))

        assertTrue(result is DeletePortfolioResult.Failure)
        val f = result as DeletePortfolioResult.Failure
        assertEquals("FK constraint", f.message)
        assertEquals(listOf("Main"), f.items.map { it.name })
        assertEquals(1, repo.deleteCalls)
        assertEquals(1, repo.getAllCalls) // safeGetAll
    }

    @Test
    fun `safeGetAll - si getAll falla, regresa lista vacia`() = runTest {
        val repo = FakePortfolioRepo(
            initial = listOf(
                Portfolio(name = "Main", description = null, isDefault = true)
            ),
            throwOnGetAll = RuntimeException("db down")
        )
        val uc = DeletePortfolioUseCase(repo)

        val result = uc.execute(DeletePortfolioCommand(id = -10))

        assertTrue(result is DeletePortfolioResult.Failure)
        val f = result as DeletePortfolioResult.Failure
        assertEquals("Id inválido.", f.message)
        assertEquals(emptyList<Portfolio>(), f.items)
        assertEquals(0, repo.deleteCalls)
        assertEquals(1, repo.getAllCalls) // safeGetAll intentó
    }

    // ------------------------------------------------------------
    // Fake PortfolioRepository (100% compatible con tu interface)
    // ------------------------------------------------------------
    private class FakePortfolioRepo(
        initial: List<Portfolio> = emptyList(),
        private val throwOnDelete: Throwable? = null,
        private val throwOnGetAll: Throwable? = null
    ) : PortfolioRepository {

        private val itemsById = linkedMapOf<Long, Portfolio>()
        private var nextId: Long = 1L
        private var defaultId: Long? = null

        var deleteCalls = 0
        var getAllCalls = 0
        var lastDeletedId: Long? = null

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
            // No-op compatible
        }

        override suspend fun delete(portfolioId: Long) {
            deleteCalls++
            lastDeletedId = portfolioId
            throwOnDelete?.let { throw it }

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
        }
    }
}