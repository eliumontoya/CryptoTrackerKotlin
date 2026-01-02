package info.eliumontoyasadec.cryptotracker.interactor

import info.eliumontoyasadec.cryptotracker.domain.interactor.portfolio.SetDefaultPortfolioCommand
import info.eliumontoyasadec.cryptotracker.domain.interactor.portfolio.SetDefaultPortfolioResult
import info.eliumontoyasadec.cryptotracker.domain.interactor.portfolio.SetDefaultPortfolioUseCase
import info.eliumontoyasadec.cryptotracker.domain.model.Portfolio
import info.eliumontoyasadec.cryptotracker.domain.repositories.PortfolioRepository
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Test

class SetDefaultPortfolioUseCaseTest {

    @Test
    fun `failure - id invalido`() = runTest {
        val repo = FakePortfolioRepo(
            initial = listOf(
                Portfolio(portfolioId = 1, name = "Main", description = null, isDefault = true)
            )
        )
        val uc = SetDefaultPortfolioUseCase(repo)

        val result = uc.execute(SetDefaultPortfolioCommand(id = 0))

        assertTrue(result is SetDefaultPortfolioResult.Failure)
        assertEquals("Id inválido.", (result as SetDefaultPortfolioResult.Failure).message)
        assertEquals(0, repo.setDefaultCalls)
        assertEquals(0, repo.getAllCalls)
    }

    @Test
    fun `success - setDefault llama repo y devuelve getAll`() = runTest {
        val repo = FakePortfolioRepo(
            initial = listOf(
                Portfolio(portfolioId = 1, name = "Main", description = null, isDefault = true),
                Portfolio(portfolioId = 2, name = "Trading", description = null, isDefault = false)
            )
        )
        val uc = SetDefaultPortfolioUseCase(repo)

        val result = uc.execute(SetDefaultPortfolioCommand(id = 2))

        assertTrue(result is SetDefaultPortfolioResult.Success)
        val s = result as SetDefaultPortfolioResult.Success

        assertEquals(1, repo.setDefaultCalls)
        assertEquals(2L, repo.lastSetDefaultId)
        assertEquals(1, repo.getAllCalls)

        val defaults = s.items.filter { it.isDefault }
        assertEquals(1, defaults.size)
        assertEquals(2L, defaults.first().portfolioId)
    }

    @Test
    fun `failure - si repo setDefault lanza excepcion`() = runTest {
        val repo = FakePortfolioRepo(
            initial = listOf(
                Portfolio(portfolioId = 1, name = "Main", description = null, isDefault = true)
            ),
            throwOnSetDefault = IllegalStateException("boom")
        )
        val uc = SetDefaultPortfolioUseCase(repo)

        val result = uc.execute(SetDefaultPortfolioCommand(id = 1))

        assertTrue(result is SetDefaultPortfolioResult.Failure)
        assertEquals("boom", (result as SetDefaultPortfolioResult.Failure).message)
        assertEquals(1, repo.setDefaultCalls)
        assertEquals(0, repo.getAllCalls) // no llega
    }

    @Test
    fun `failure - mensaje nulo cae en Fallo desconocido`() = runTest {
        val repo = FakePortfolioRepo(
            initial = listOf(
                Portfolio(portfolioId = 1, name = "Main", description = null, isDefault = true)
            ),
            throwOnSetDefault = RuntimeException()
        )
        val uc = SetDefaultPortfolioUseCase(repo)

        val result = uc.execute(SetDefaultPortfolioCommand(id = 1))

        assertTrue(result is SetDefaultPortfolioResult.Failure)
        assertEquals("Fallo desconocido", (result as SetDefaultPortfolioResult.Failure).message)
    }

    // ------------------------------------------------------------
    // Fake PortfolioRepository (alineado a PortfolioRepository.kt)
    // ------------------------------------------------------------
    private class FakePortfolioRepo(
        initial: List<Portfolio> = emptyList(),
        private val throwOnSetDefault: Throwable? = null
    ) : PortfolioRepository {

        private val itemsById = linkedMapOf<Long, Portfolio>().apply {
            initial.forEach { put(it.portfolioId, it) }
        }

        var setDefaultCalls = 0
        var getAllCalls = 0
        var lastSetDefaultId: Long? = null

        override suspend fun exists(portfolioId: Long): Boolean =
            itemsById.containsKey(portfolioId)

        override suspend fun insert(portfolio: Portfolio): Long {
            val newId = (itemsById.keys.maxOrNull() ?: 0L) + 1L
            itemsById[newId] = portfolio.copy(portfolioId = newId)
            return newId
        }

        override suspend fun findById(portfolioId: Long): Portfolio? =
            itemsById[portfolioId]

        override suspend fun getAll(): List<Portfolio> {
            getAllCalls++
            return itemsById.values.toList()
        }

        override suspend fun getDefault(): Portfolio? =
            itemsById.values.firstOrNull { it.isDefault }

        override suspend fun update(portfolio: Portfolio) {
            itemsById[portfolio.portfolioId] = portfolio
        }

        override suspend fun delete(portfolioId: Long) {
            itemsById.remove(portfolioId)
        }

        override suspend fun delete(portfolio: Portfolio) {
            itemsById.remove(portfolio.portfolioId)
        }

        override suspend fun isDefault(portfolioId: Long): Boolean =
            itemsById[portfolioId]?.isDefault == true

        override suspend fun setDefault(portfolioId: Long) {
            setDefaultCalls++
            lastSetDefaultId = portfolioId
            throwOnSetDefault?.let { throw it }

            // desmarca todos y marca este
            itemsById.entries.forEach { (id, p) ->
                itemsById[id] = p.copy(isDefault = (id == portfolioId))
            }
        }
    }
}