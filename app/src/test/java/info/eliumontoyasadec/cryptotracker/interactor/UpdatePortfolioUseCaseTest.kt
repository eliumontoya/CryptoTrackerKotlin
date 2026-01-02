package info.eliumontoyasadec.cryptotracker.interactor

import info.eliumontoyasadec.cryptotracker.domain.interactor.portfolio.UpdatePortfolioCommand
import info.eliumontoyasadec.cryptotracker.domain.interactor.portfolio.UpdatePortfolioResult
import info.eliumontoyasadec.cryptotracker.domain.interactor.portfolio.UpdatePortfolioUseCase
import info.eliumontoyasadec.cryptotracker.domain.model.Portfolio
import info.eliumontoyasadec.cryptotracker.domain.repositories.PortfolioRepository
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Test

class UpdatePortfolioUseCaseTest {

    @Test
    fun `validation error - id invalido`() = runTest {
        val repo = FakePortfolioRepo()
        val uc = UpdatePortfolioUseCase(repo)

        val result = uc.execute(
            UpdatePortfolioCommand(
                id = 0,
                nameRaw = "Main",
                descriptionRaw = null,
                makeDefault = false
            )
        )

        assertTrue(result is UpdatePortfolioResult.ValidationError)
        assertEquals("Id inválido.", (result as UpdatePortfolioResult.ValidationError).message)
        assertEquals(0, repo.updateCalls)
        assertEquals(0, repo.getAllCalls)
    }

    @Test
    fun `validation error - nombre obligatorio`() = runTest {
        val repo = FakePortfolioRepo()
        val uc = UpdatePortfolioUseCase(repo)

        val result = uc.execute(
            UpdatePortfolioCommand(
                id = 1,
                nameRaw = "   ",
                descriptionRaw = "algo",
                makeDefault = false
            )
        )

        assertTrue(result is UpdatePortfolioResult.ValidationError)
        assertEquals("El nombre es obligatorio.", (result as UpdatePortfolioResult.ValidationError).message)
        assertEquals(0, repo.updateCalls)
        assertEquals(0, repo.getAllCalls)
    }

    @Test
    fun `success - trimea name, description blank a null y respeta makeDefault`() = runTest {
        val repo = FakePortfolioRepo(
            initial = listOf(
                Portfolio(portfolioId = 1, name = "Main", description = null, isDefault = false),
                Portfolio(portfolioId = 2, name = "Trading", description = null, isDefault = true)
            )
        )
        val uc = UpdatePortfolioUseCase(repo)

        val result = uc.execute(
            UpdatePortfolioCommand(
                id = 1,
                nameRaw = "  Nuevo  ",
                descriptionRaw = "   ",
                makeDefault = true
            )
        )

        assertTrue(result is UpdatePortfolioResult.Success)
        val s = result as UpdatePortfolioResult.Success

        assertEquals(1, repo.updateCalls)
        assertEquals(1, repo.getAllCalls)

        val updated = repo.lastUpdated!!
        assertEquals(1L, updated.portfolioId)
        assertEquals("Nuevo", updated.name)
        assertNull(updated.description)
        assertTrue(updated.isDefault)

        // la lista final tiene el update reflejado
        val p1 = s.items.first { it.portfolioId == 1L }
        assertEquals("Nuevo", p1.name)
        assertTrue(p1.isDefault)
    }

    @Test
    fun `success - description no blank se conserva trim`() = runTest {
        val repo = FakePortfolioRepo(
            initial = listOf(
                Portfolio(portfolioId = 1, name = "Main", description = null, isDefault = false)
            )
        )
        val uc = UpdatePortfolioUseCase(repo)

        val result = uc.execute(
            UpdatePortfolioCommand(
                id = 1,
                nameRaw = "Main",
                descriptionRaw = "  Desc  ",
                makeDefault = false
            )
        )

        assertTrue(result is UpdatePortfolioResult.Success)
        assertEquals("Desc", repo.lastUpdated?.description)
    }

    @Test
    fun `failure - si repo update lanza excepcion`() = runTest {
        val repo = FakePortfolioRepo(
            initial = listOf(
                Portfolio(portfolioId = 1, name = "Main", description = null, isDefault = false)
            ),
            throwOnUpdate = IllegalStateException("boom")
        )
        val uc = UpdatePortfolioUseCase(repo)

        val result = uc.execute(
            UpdatePortfolioCommand(
                id = 1,
                nameRaw = "Main",
                descriptionRaw = null,
                makeDefault = false
            )
        )

        assertTrue(result is UpdatePortfolioResult.Failure)
        assertEquals("boom", (result as UpdatePortfolioResult.Failure).message)
        assertEquals(1, repo.updateCalls)
        assertEquals(0, repo.getAllCalls) // no llega
    }

    @Test
    fun `failure - mensaje nulo cae en Fallo desconocido`() = runTest {
        val repo = FakePortfolioRepo(
            initial = listOf(
                Portfolio(portfolioId = 1, name = "Main", description = null, isDefault = false)
            ),
            throwOnUpdate = RuntimeException()
        )
        val uc = UpdatePortfolioUseCase(repo)

        val result = uc.execute(
            UpdatePortfolioCommand(
                id = 1,
                nameRaw = "Main",
                descriptionRaw = null,
                makeDefault = false
            )
        )

        assertTrue(result is UpdatePortfolioResult.Failure)
        assertEquals("Fallo desconocido", (result as UpdatePortfolioResult.Failure).message)
    }

    // ------------------------------------------------------------
    // Fake PortfolioRepository (alineado a PortfolioRepository.kt)
    // ------------------------------------------------------------
    private class FakePortfolioRepo(
        initial: List<Portfolio> = emptyList(),
        private val throwOnUpdate: Throwable? = null
    ) : PortfolioRepository {

        private val itemsById = linkedMapOf<Long, Portfolio>().apply {
            initial.forEach { put(it.portfolioId, it) }
        }

        var updateCalls = 0
        var getAllCalls = 0
        var lastUpdated: Portfolio? = null

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
            updateCalls++
            lastUpdated = portfolio
            throwOnUpdate?.let { throw it }
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
            itemsById.entries.forEach { (id, p) ->
                itemsById[id] = p.copy(isDefault = (id == portfolioId))
            }
        }
    }
}