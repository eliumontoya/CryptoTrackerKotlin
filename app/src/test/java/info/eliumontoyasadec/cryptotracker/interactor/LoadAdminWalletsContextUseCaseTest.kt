// LoadAdminWalletsContextUseCaseTest.kt
package info.eliumontoyasadec.cryptotracker.interactor

import info.eliumontoyasadec.cryptotracker.domain.interactor.wallet.LoadAdminWalletsContextResult
import info.eliumontoyasadec.cryptotracker.domain.interactor.wallet.LoadAdminWalletsContextUseCase
import info.eliumontoyasadec.cryptotracker.domain.model.Portfolio
import info.eliumontoyasadec.cryptotracker.domain.model.Wallet
import info.eliumontoyasadec.cryptotracker.domain.repositories.PortfolioRepository
import info.eliumontoyasadec.cryptotracker.domain.repositories.WalletRepository
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Test

class LoadAdminWalletsContextUseCaseTest {

    @Test
    fun `success - sin portfolios devuelve selected null y wallets vacio`() = runTest {
        val walletRepo = FakeWalletRepo()
        val portfolioRepo = FakePortfolioRepo(initial = emptyList())
        val uc = LoadAdminWalletsContextUseCase(walletRepo, portfolioRepo)

        val result = uc.execute()

        assertTrue(result is LoadAdminWalletsContextResult.Success)
        val s = result as LoadAdminWalletsContextResult.Success

        assertEquals(emptyList<Portfolio>(), s.portfolios)
        assertEquals(null, s.selectedPortfolioId)
        assertEquals(emptyList<Wallet>(), s.wallets)
        assertEquals(0, walletRepo.getByPortfolioCalls) // no hay selectedId
        assertEquals(1, portfolioRepo.getAllCalls)
    }

    @Test
    fun `success - con default, selected es default y se ordena primero`() = runTest {
        val walletRepo = FakeWalletRepo(
            initial = listOf(
                Wallet(walletId = 1, portfolioId = 2, name = "W2", description = "description",isMain = true),
                Wallet(walletId = 2, portfolioId = 1, name = "W1",description = "description", isMain = true)
            )
        )
        val portfolioRepo = FakePortfolioRepo(
            initial = listOf(
                Portfolio(portfolioId = 1, name = "P1", description = null, isDefault = false),
                Portfolio(portfolioId = 2, name = "P2", description = null, isDefault = true),
                Portfolio(portfolioId = 3, name = "P3", description = null, isDefault = false)
            )
        )
        val uc = LoadAdminWalletsContextUseCase(walletRepo, portfolioRepo)

        val result = uc.execute()

        assertTrue(result is LoadAdminWalletsContextResult.Success)
        val s = result as LoadAdminWalletsContextResult.Success

        assertEquals(2L, s.selectedPortfolioId)
        assertEquals(3, s.portfolios.size)
        assertEquals(2L, s.portfolios.first().portfolioId) // default primero
        assertEquals(listOf(1L), s.wallets.map { it.walletId }) // wallets del portfolio 2

        assertEquals(1, walletRepo.getByPortfolioCalls)
        assertEquals(2L, walletRepo.lastGetByPortfolioId)
        assertEquals(1, portfolioRepo.getAllCalls)
    }

    @Test
    fun `success - sin default, selected es el primero`() = runTest {
        val walletRepo = FakeWalletRepo(
            initial = listOf(
                Wallet(walletId = 10, portfolioId = 7, name = "W7", description = null,isMain = false)
            )
        )
        val portfolioRepo = FakePortfolioRepo(
            initial = listOf(
                Portfolio(portfolioId = 7, name = "P7", description = null, isDefault = false),
                Portfolio(portfolioId = 8, name = "P8", description = null, isDefault = false)
            )
        )
        val uc = LoadAdminWalletsContextUseCase(walletRepo, portfolioRepo)

        val result = uc.execute()

        assertTrue(result is LoadAdminWalletsContextResult.Success)
        val s = result as LoadAdminWalletsContextResult.Success

        assertEquals(7L, s.selectedPortfolioId)
        assertEquals(listOf(10L), s.wallets.map { it.walletId })
        assertEquals(1, walletRepo.getByPortfolioCalls)
    }

    @Test
    fun `failure - si portfolioRepo getAll lanza exception`() = runTest {
        val walletRepo = FakeWalletRepo()
        val portfolioRepo = FakePortfolioRepo(throwOnGetAll = RuntimeException("boom"))
        val uc = LoadAdminWalletsContextUseCase(walletRepo, portfolioRepo)

        val result = uc.execute()

        assertTrue(result is LoadAdminWalletsContextResult.Failure)
        assertEquals("boom", (result as LoadAdminWalletsContextResult.Failure).message)
        assertEquals(0, walletRepo.getByPortfolioCalls)
        assertEquals(1, portfolioRepo.getAllCalls)
    }

    @Test
    fun `failure - si walletRepo getByPortfolio lanza exception`() = runTest {
        val walletRepo = FakeWalletRepo(throwOnGetByPortfolio = RuntimeException("wallet down"))
        val portfolioRepo = FakePortfolioRepo(
            initial = listOf(Portfolio(portfolioId = 1, name = "P1", description = null, isDefault = true))
        )
        val uc = LoadAdminWalletsContextUseCase(walletRepo, portfolioRepo)

        val result = uc.execute()

        assertTrue(result is LoadAdminWalletsContextResult.Failure)
        assertEquals("wallet down", (result as LoadAdminWalletsContextResult.Failure).message)
        assertEquals(1, walletRepo.getByPortfolioCalls)
        assertEquals(1, portfolioRepo.getAllCalls)
    }

    // ------------------------------------------------------------
    // Fake WalletRepository
    // ------------------------------------------------------------
    private class FakeWalletRepo(
        initial: List<Wallet> = emptyList(),
        private val throwOnGetByPortfolio: Throwable? = null
    ) : WalletRepository {

        private val itemsById = linkedMapOf<Long, Wallet>().apply {
            initial.forEach { put(it.walletId, it) }
        }

        var getByPortfolioCalls = 0
        var lastGetByPortfolioId: Long? = null

        override suspend fun exists(walletId: Long): Boolean = itemsById.containsKey(walletId)

        override suspend fun belongsToPortfolio(walletId: Long, portfolioId: Long): Boolean =
            itemsById[walletId]?.portfolioId == portfolioId

        override suspend fun insert(wallet: Wallet): Long {
            val newId = (itemsById.keys.maxOrNull() ?: 0L) + 1L
            itemsById[newId] = wallet.copy(walletId = newId)
            return newId
        }

        override suspend fun findById(walletId: Long): Wallet? = itemsById[walletId]

        override suspend fun getByPortfolio(portfolioId: Long): List<Wallet> {
            getByPortfolioCalls++
            lastGetByPortfolioId = portfolioId
            throwOnGetByPortfolio?.let { throw it }
            return itemsById.values.filter { it.portfolioId == portfolioId }
        }

        override suspend fun update(wallet: Wallet) {
            itemsById[wallet.walletId] = wallet
        }

        override suspend fun delete(walletId: Long) {
            itemsById.remove(walletId)
        }

        override suspend fun isMain(walletId: Long): Boolean =
            itemsById[walletId]?.isMain == true

        override suspend fun setMain(walletId: Long) {
            val w = itemsById[walletId] ?: return
            val pid = w.portfolioId
            itemsById.entries.forEach { (id, item) ->
                if (item.portfolioId == pid) {
                    itemsById[id] = item.copy(isMain = (id == walletId))
                }
            }
        }

        override suspend fun update(walletId: Long, name: String) {
            val w = itemsById[walletId] ?: return
            itemsById[walletId] = w.copy(name = name)
        }
    }

    // ------------------------------------------------------------
    // Fake PortfolioRepository
    // ------------------------------------------------------------
    private class FakePortfolioRepo(
        initial: List<Portfolio> = emptyList(),
        private val throwOnGetAll: Throwable? = null
    ) : PortfolioRepository {

        private val items = initial.toList()

        var getAllCalls = 0

        override suspend fun exists(portfolioId: Long): Boolean =
            items.any { it.portfolioId == portfolioId }

        override suspend fun insert(portfolio: Portfolio): Long = portfolio.portfolioId

        override suspend fun findById(portfolioId: Long): Portfolio? =
            items.firstOrNull { it.portfolioId == portfolioId }

        override suspend fun getAll(): List<Portfolio> {
            getAllCalls++
            throwOnGetAll?.let { throw it }
            return items
        }

        override suspend fun getDefault(): Portfolio? =
            items.firstOrNull { it.isDefault }

        override suspend fun update(portfolio: Portfolio) {}

        override suspend fun delete(portfolioId: Long) {}

        override suspend fun delete(portfolio: Portfolio) {}

        override suspend fun isDefault(portfolioId: Long): Boolean =
            items.firstOrNull { it.portfolioId == portfolioId }?.isDefault == true

        override suspend fun setDefault(portfolioId: Long) {}
    }
}