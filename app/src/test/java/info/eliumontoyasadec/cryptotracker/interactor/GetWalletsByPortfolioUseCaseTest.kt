package info.eliumontoyasadec.cryptotracker.interactor

import info.eliumontoyasadec.cryptotracker.domain.interactor.wallet.GetWalletsByPortfolioCommand
import info.eliumontoyasadec.cryptotracker.domain.interactor.wallet.GetWalletsByPortfolioUseCase
import info.eliumontoyasadec.cryptotracker.domain.model.Wallet
import info.eliumontoyasadec.cryptotracker.domain.repositories.WalletRepository
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class GetWalletsByPortfolioUseCaseTest {

    @Test
    fun `execute delega al repo getByPortfolio`() = runTest {
        val repo = FakeWalletRepo(
            initial = listOf(
                Wallet(walletId = 1, portfolioId = 10, name = "Wallet A", description = "description",isMain = true),
                Wallet(walletId = 2, portfolioId = 10, name = "Wallet B", description = "description",isMain = false),
                Wallet(walletId = 3, portfolioId = 99, name = "Other", description = "description",isMain = false)
            )
        )
        val uc = GetWalletsByPortfolioUseCase(repo)

        val result = uc.execute(GetWalletsByPortfolioCommand(portfolioId = 10))

        assertEquals(2, result.size)
        assertEquals(listOf(1L, 2L), result.map { it.walletId }.sorted())
        assertEquals(1, repo.getByPortfolioCalls)
        assertEquals(10L, repo.lastGetByPortfolioId)
    }

    // ------------------------------------------------------------
    // Fake WalletRepository (100% compatible con WalletRepository.kt)
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
            // desmarca main en el mismo portfolio y marca este
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