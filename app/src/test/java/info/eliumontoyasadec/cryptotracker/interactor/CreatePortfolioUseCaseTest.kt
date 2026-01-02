package info.eliumontoyasadec.cryptotracker.interactor

import info.eliumontoyasadec.cryptotracker.domain.interactor.portfolio.CreatePortfolioCommand
import info.eliumontoyasadec.cryptotracker.domain.interactor.portfolio.CreatePortfolioResult
import info.eliumontoyasadec.cryptotracker.domain.interactor.portfolio.CreatePortfolioUseCase
import info.eliumontoyasadec.cryptotracker.domain.model.Portfolio
import info.eliumontoyasadec.cryptotracker.domain.repositories.PortfolioRepository
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Test

class CreatePortfolioUseCaseTest {

    @Test
    fun `validation error si name esta vacio`() = runTest {
        val repo = FakePortfolioRepo()
        val uc = CreatePortfolioUseCase(repo)

        val result = uc.execute(
            CreatePortfolioCommand(
                nameRaw = "   ",
                descriptionRaw = "algo",
                makeDefault = false
            )
        )

        assertTrue(result is CreatePortfolioResult.ValidationError)
        assertEquals("El nombre es obligatorio.", (result as CreatePortfolioResult.ValidationError).message)
        assertEquals(0, repo.insertCalls)
        assertEquals(0, repo.getAllCalls)
    }

    @Test
    fun `success - trimea name y description y setea isDefault segun cmd`() = runTest {
        val repo = FakePortfolioRepo(
            initial = listOf(
                Portfolio(name = "Old", description = null, isDefault = false)
            )
        )
        val uc = CreatePortfolioUseCase(repo)

        val result = uc.execute(
            CreatePortfolioCommand(
                nameRaw = "  Nuevo  ",
                descriptionRaw = "  Descripción  ",
                makeDefault = true
            )
        )

        assertTrue(result is CreatePortfolioResult.Success)
        val s = result as CreatePortfolioResult.Success

        assertEquals(1, repo.insertCalls)
        assertEquals(1, repo.getAllCalls)

        assertEquals("Nuevo", repo.lastInserted?.name)
        assertEquals("Descripción", repo.lastInserted?.description)
        assertEquals(true, repo.lastInserted?.isDefault)

        assertEquals(2, s.items.size)
        assertTrue(s.items.any { it.name == "Nuevo" })
    }

    @Test
    fun `success - description blank se guarda como null`() = runTest {
        val repo = FakePortfolioRepo()
        val uc = CreatePortfolioUseCase(repo)

        val result = uc.execute(
            CreatePortfolioCommand(
                nameRaw = "Main",
                descriptionRaw = "   ",
                makeDefault = false
            )
        )

        assertTrue(result is CreatePortfolioResult.Success)
        assertEquals(null, repo.lastInserted?.description)
    }

    @Test
    fun `failure - si repo insert lanza excepcion`() = runTest {
        val repo = FakePortfolioRepo(throwOnInsert = RuntimeException("boom"))
        val uc = CreatePortfolioUseCase(repo)

        val result = uc.execute(
            CreatePortfolioCommand(
                nameRaw = "Main",
                descriptionRaw = null,
                makeDefault = false
            )
        )

        assertTrue(result is CreatePortfolioResult.Failure)
        assertEquals("boom", (result as CreatePortfolioResult.Failure).message)
        assertEquals(1, repo.insertCalls) // lo intentó
        assertEquals(0, repo.getAllCalls) // no llegó
    }

    // ------------------------------------------------------------
    // Fake PortfolioRepository (100% compatible con tu interface)
    // ------------------------------------------------------------
    private class FakePortfolioRepo(
        initial: List<Portfolio> = emptyList(),
        private val throwOnInsert: Throwable? = null,
        private val throwOnGetAll: Throwable? = null
    ) : PortfolioRepository {

        private val itemsById = linkedMapOf<Long, Portfolio>()
        private var nextId: Long = 1L
        private var defaultId: Long? = null

        var insertCalls = 0
        var getAllCalls = 0
        var lastInserted: Portfolio? = null

        init {
            initial.forEach { p ->
                val id = nextId++
                itemsById[id] = p
                if (p.isDefault) defaultId = id
            }
        }

        override suspend fun exists(portfolioId: Long): Boolean = itemsById.containsKey(portfolioId)

        override suspend fun insert(portfolio: Portfolio): Long {
            insertCalls++
            lastInserted = portfolio
            throwOnInsert?.let { throw it }

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