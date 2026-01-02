package info.eliumontoyasadec.cryptotracker.interactor

import info.eliumontoyasadec.cryptotracker.domain.interactor.wallet.SetMainWalletCommand
import info.eliumontoyasadec.cryptotracker.domain.interactor.wallet.SetMainWalletResult
import info.eliumontoyasadec.cryptotracker.domain.interactor.wallet.SetMainWalletUseCase
import info.eliumontoyasadec.cryptotracker.domain.model.Wallet
import info.eliumontoyasadec.cryptotracker.domain.repositories.WalletRepository
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Test

class SetMainWalletUseCaseTest {

    @Test
    fun `failure - datos invalidos`() = runTest {
        val repo = FakeWalletRepo(
            initial = listOf(
                Wallet(walletId = 1, portfolioId = 10, name = "A", description = "", isMain = true)
            )
        )
        val uc = SetMainWalletUseCase(repo)

        val result = uc.execute(SetMainWalletCommand(portfolioId = 10, walletId = 0))

        assertTrue(result is SetMainWalletResult.Failure)
        assertEquals("Datos inválidos.", (result as SetMainWalletResult.Failure).message)
        assertEquals(0, repo.setMainCalls)
        assertEquals(0, repo.getByPortfolioCalls)
    }

    @Test
    fun `success - setMain y devuelve wallets del portfolio`() = runTest {
        val repo = FakeWalletRepo(
            initial = listOf(
                Wallet(walletId = 1, portfolioId = 10, name = "A", description = "", isMain = true),
                Wallet(walletId = 2, portfolioId = 10, name = "B", description = "", isMain = false),
                Wallet(walletId = 3, portfolioId = 99, name = "Other", description = "", isMain = true)
            )
        )
        val uc = SetMainWalletUseCase(repo)

        val result = uc.execute(SetMainWalletCommand(portfolioId = 10, walletId = 2))

        assertTrue(result is SetMainWalletResult.Success)
        val s = result as SetMainWalletResult.Success

        assertEquals(1, repo.setMainCalls)
        assertEquals(2L, repo.lastSetMainId)
        assertEquals(1, repo.getByPortfolioCalls)
        assertEquals(10L, repo.lastGetByPortfolioId)

        // Solo wallets del portfolioId=10
        assertEquals(listOf(1L, 2L), s.items.map { it.walletId }.sorted())

        // El main debe ser walletId=2 dentro del portfolio 10
        val mains = s.items.filter { it.isMain }
        assertEquals(1, mains.size)
        assertEquals(2L, mains.first().walletId)
    }

    @Test
    fun `failure - si repo setMain lanza excepcion`() = runTest {
        val repo = FakeWalletRepo(
            initial = listOf(
                Wallet(walletId = 1, portfolioId = 10, name = "A", description = "", isMain = true)
            ),
            throwOnSetMain = IllegalStateException("boom")
        )
        val uc = SetMainWalletUseCase(repo)

        val result = uc.execute(SetMainWalletCommand(portfolioId = 10, walletId = 1))

        assertTrue(result is SetMainWalletResult.Failure)
        assertEquals("boom", (result as SetMainWalletResult.Failure).message)
        assertEquals(1, repo.setMainCalls)
        assertEquals(0, repo.getByPortfolioCalls) // no llega
    }

    @Test
    fun `failure - mensaje nulo cae en Fallo desconocido`() = runTest {
        val repo = FakeWalletRepo(
            initial = listOf(
                Wallet(walletId = 1, portfolioId = 10, name = "A", description = "", isMain = true)
            ),
            throwOnSetMain = RuntimeException()
        )
        val uc = SetMainWalletUseCase(repo)

        val result = uc.execute(SetMainWalletCommand(portfolioId = 10, walletId = 1))

        assertTrue(result is SetMainWalletResult.Failure)
        assertEquals("Fallo desconocido", (result as SetMainWalletResult.Failure).message)
    }

    // ------------------------------------------------------------
    // Fake WalletRepository (compatible con WalletRepository.kt)
    // ------------------------------------------------------------
    private class FakeWalletRepo(
        initial: List<Wallet> = emptyList(),
        private val throwOnSetMain: Throwable? = null,
        private val throwOnGetByPortfolio: Throwable? = null
    ) : WalletRepository {

        private val itemsById = linkedMapOf<Long, Wallet>().apply {
            initial.forEach { put(it.walletId, it) }
        }

        var setMainCalls = 0
        var getByPortfolioCalls = 0
        var lastSetMainId: Long? = null
        var lastGetByPortfolioId: Long? = null

        override suspend fun exists(walletId: Long): Boolean =
            itemsById.containsKey(walletId)

        override suspend fun belongsToPortfolio(walletId: Long, portfolioId: Long): Boolean =
            itemsById[walletId]?.portfolioId == portfolioId

        override suspend fun insert(wallet: Wallet): Long {
            val newId = (itemsById.keys.maxOrNull() ?: 0L) + 1L
            itemsById[newId] = wallet.copy(walletId = newId)
            return newId
        }

        override suspend fun findById(walletId: Long): Wallet? =
            itemsById[walletId]

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
            setMainCalls++
            lastSetMainId = walletId
            throwOnSetMain?.let { throw it }

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
}