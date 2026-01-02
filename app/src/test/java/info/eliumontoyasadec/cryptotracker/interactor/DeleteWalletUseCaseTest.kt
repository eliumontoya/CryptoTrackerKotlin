package info.eliumontoyasadec.cryptotracker.interactor

import info.eliumontoyasadec.cryptotracker.domain.interactor.wallet.DeleteWalletCommand
import info.eliumontoyasadec.cryptotracker.domain.interactor.wallet.DeleteWalletResult
import info.eliumontoyasadec.cryptotracker.domain.interactor.wallet.DeleteWalletUseCase
import info.eliumontoyasadec.cryptotracker.domain.model.Wallet
import info.eliumontoyasadec.cryptotracker.domain.repositories.WalletRepository
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Test

class DeleteWalletUseCaseTest {

    @Test
    fun `failure - datos invalidos regresa safeGet`() = runTest {
        val repo = FakeWalletRepo(
            initial = listOf(
                Wallet(walletId = 1, portfolioId = 10, name = "Wallet A", description = "description",isMain = true)
            )
        )
        val uc = DeleteWalletUseCase(repo)

        val result = uc.execute(DeleteWalletCommand(portfolioId = 10, walletId = 0))

        assertTrue(result is DeleteWalletResult.Failure)
        val f = result as DeleteWalletResult.Failure
        assertEquals("Datos inválidos.", f.message)
        assertEquals(listOf(1L), f.items.map { it.walletId })
        assertEquals(0, repo.deleteCalls)
        assertEquals(1, repo.getByPortfolioCalls) // safeGet
    }

    @Test
    fun `success - borra y devuelve lista actualizada`() = runTest {
        val repo = FakeWalletRepo(
            initial = listOf(
                Wallet(walletId = 1, portfolioId = 10, name = "Wallet A", description = "description",isMain = true),
                Wallet(walletId = 2, portfolioId = 10, name = "Wallet B", description = "description",isMain = false),
                Wallet(walletId = 3, portfolioId = 99, name = "Other",description = "description", isMain = false)
            )
        )
        val uc = DeleteWalletUseCase(repo)

        val result = uc.execute(DeleteWalletCommand(portfolioId = 10, walletId = 2))

        assertTrue(result is DeleteWalletResult.Success)
        val s = result as DeleteWalletResult.Success

        assertEquals(1, repo.deleteCalls)
        assertEquals(2L, repo.lastDeletedId)
        assertEquals(1, repo.getByPortfolioCalls)

        assertEquals(listOf(1L), s.items.map { it.walletId }) // quedó solo walletId=1 en portfolio 10
    }

    @Test
    fun `failure - si delete lanza exception regresa safeGet`() = runTest {
        val repo = FakeWalletRepo(
            initial = listOf(
                Wallet(walletId = 1, portfolioId = 10, name = "Wallet A", description = "description",isMain = true)
            ),
            throwOnDelete = IllegalStateException("FK constraint")
        )
        val uc = DeleteWalletUseCase(repo)

        val result = uc.execute(DeleteWalletCommand(portfolioId = 10, walletId = 1))

        assertTrue(result is DeleteWalletResult.Failure)
        val f = result as DeleteWalletResult.Failure
        assertEquals("FK constraint", f.message)
        assertEquals(listOf(1L), f.items.map { it.walletId }) // safeGet no borró
        assertEquals(1, repo.deleteCalls)
        assertEquals(1, repo.getByPortfolioCalls)
    }

    @Test
    fun `safeGet - si getByPortfolio falla, regresa lista vacia`() = runTest {
        val repo = FakeWalletRepo(
            initial = listOf(
                Wallet(walletId = 1, portfolioId = 10, name = "Wallet A", description = "description",isMain = true)
            ),
            throwOnGetByPortfolio = RuntimeException("db down")
        )
        val uc = DeleteWalletUseCase(repo)

        val result = uc.execute(DeleteWalletCommand(portfolioId = 10, walletId = 0))

        assertTrue(result is DeleteWalletResult.Failure)
        val f = result as DeleteWalletResult.Failure
        assertEquals("Datos inválidos.", f.message)
        assertEquals(emptyList<Wallet>(), f.items)
        assertEquals(0, repo.deleteCalls)
        assertEquals(1, repo.getByPortfolioCalls) // safeGet intentó
    }

    // ------------------------------------------------------------
    // Fake WalletRepository (100% compatible con WalletRepository.kt)
    // ------------------------------------------------------------
    private class FakeWalletRepo(
        initial: List<Wallet> = emptyList(),
        private val throwOnDelete: Throwable? = null,
        private val throwOnGetByPortfolio: Throwable? = null
    ) : WalletRepository {

        private val itemsById = linkedMapOf<Long, Wallet>().apply {
            initial.forEach { put(it.walletId, it) }
        }

        var deleteCalls = 0
        var getByPortfolioCalls = 0
        var lastDeletedId: Long? = null

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
            throwOnGetByPortfolio?.let { throw it }
            return itemsById.values.filter { it.portfolioId == portfolioId }
        }

        override suspend fun update(wallet: Wallet) {
            itemsById[wallet.walletId] = wallet
        }

        override suspend fun delete(walletId: Long) {
            deleteCalls++
            lastDeletedId = walletId
            throwOnDelete?.let { throw it }
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
}