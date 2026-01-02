package info.eliumontoyasadec.cryptotracker.interactor

import info.eliumontoyasadec.cryptotracker.domain.interactor.wallet.UpsertWalletCommand
import info.eliumontoyasadec.cryptotracker.domain.interactor.wallet.UpsertWalletResult
import info.eliumontoyasadec.cryptotracker.domain.interactor.wallet.UpsertWalletUseCase
import info.eliumontoyasadec.cryptotracker.domain.model.Wallet
import info.eliumontoyasadec.cryptotracker.domain.repositories.WalletRepository
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Test

class UpsertWalletUseCaseTest {

    @Test
    fun `validation error - portfolio invalido`() = runTest {
        val repo = FakeWalletRepo()
        val uc = UpsertWalletUseCase(repo)

        val result = uc.execute(
            UpsertWalletCommand(
                portfolioId = 0,
                walletId = null,
                nameRaw = "Wallet",
                makeMain = false
            )
        )

        assertTrue(result is UpsertWalletResult.ValidationError)
        assertEquals("Portfolio inválido.", (result as UpsertWalletResult.ValidationError).message)
        assertEquals(0, repo.insertCalls)
        assertEquals(0, repo.updateNameCalls)
        assertEquals(0, repo.getByPortfolioCalls)
    }

    @Test
    fun `validation error - nombre vacio`() = runTest {
        val repo = FakeWalletRepo()
        val uc = UpsertWalletUseCase(repo)

        val result = uc.execute(
            UpsertWalletCommand(
                portfolioId = 10,
                walletId = null,
                nameRaw = "   ",
                makeMain = false
            )
        )

        assertTrue(result is UpsertWalletResult.ValidationError)
        assertEquals("El nombre no puede estar vacío.", (result as UpsertWalletResult.ValidationError).message)
        assertEquals(0, repo.insertCalls)
        assertEquals(0, repo.updateNameCalls)
        assertEquals(0, repo.getByPortfolioCalls)
    }

    @Test
    fun `success create - inserta Wallet con walletId 0, description vacia y devuelve items`() = runTest {
        val repo = FakeWalletRepo(
            initial = listOf(
                Wallet(walletId = 1, portfolioId = 10, name = "A", description = "", isMain = true)
            )
        )
        val uc = UpsertWalletUseCase(repo)

        val result = uc.execute(
            UpsertWalletCommand(
                portfolioId = 10,
                walletId = null,              // create
                nameRaw = "  Nueva  ",
                makeMain = false
            )
        )

        assertTrue(result is UpsertWalletResult.Success)
        val s = result as UpsertWalletResult.Success

        assertEquals(1, repo.insertCalls)
        assertEquals("Nueva", repo.lastInserted?.name)
        assertEquals("", repo.lastInserted?.description) // así lo construye el UC
        assertEquals(0L, repo.lastInserted?.walletId)    // así lo construye el UC

        assertEquals(1, repo.getByPortfolioCalls)
        assertEquals(10L, repo.lastGetByPortfolioId)

        assertEquals(2, s.items.size)
        assertTrue(s.items.any { it.name == "Nueva" })
    }

    @Test
    fun `success create - makeMain true llama setMain con newId`() = runTest {
        val repo = FakeWalletRepo()
        val uc = UpsertWalletUseCase(repo)

        val result = uc.execute(
            UpsertWalletCommand(
                portfolioId = 10,
                walletId = null,
                nameRaw = "Nueva",
                makeMain = true
            )
        )

        assertTrue(result is UpsertWalletResult.Success)
        assertEquals(1, repo.insertCalls)
        assertEquals(1, repo.setMainCalls)
        assertEquals(repo.lastInsertedId, repo.lastSetMainId) // setMain(newId)
    }

    @Test
    fun `success update - walletId no null llama update(id,name) y devuelve items`() = runTest {
        val repo = FakeWalletRepo(
            initial = listOf(
                Wallet(walletId = 5, portfolioId = 10, name = "Old", description = "", isMain = false)
            )
        )
        val uc = UpsertWalletUseCase(repo)

        val result = uc.execute(
            UpsertWalletCommand(
                portfolioId = 10,
                walletId = 5,                 // update
                nameRaw = "  Updated  ",
                makeMain = false
            )
        )

        assertTrue(result is UpsertWalletResult.Success)
        val s = result as UpsertWalletResult.Success

        assertEquals(0, repo.insertCalls)
        assertEquals(1, repo.updateNameCalls)
        assertEquals(5L, repo.lastUpdateNameId)
        assertEquals("Updated", repo.lastUpdateName)

        assertEquals(1, repo.getByPortfolioCalls)
        assertEquals(listOf("Updated"), s.items.map { it.name })
    }

    @Test
    fun `success update - makeMain true llama setMain(editingId)`() = runTest {
        val repo = FakeWalletRepo(
            initial = listOf(
                Wallet(walletId = 5, portfolioId = 10, name = "Old", description = "", isMain = false),
                Wallet(walletId = 6, portfolioId = 10, name = "Other", description = "", isMain = true)
            )
        )
        val uc = UpsertWalletUseCase(repo)

        val result = uc.execute(
            UpsertWalletCommand(
                portfolioId = 10,
                walletId = 5,
                nameRaw = "Old",
                makeMain = true
            )
        )

        assertTrue(result is UpsertWalletResult.Success)
        assertEquals(1, repo.setMainCalls)
        assertEquals(5L, repo.lastSetMainId)

        val s = result as UpsertWalletResult.Success
        val mains = s.items.filter { it.isMain }
        assertEquals(1, mains.size)
        assertEquals(5L, mains.first().walletId)
    }

    @Test
    fun `failure - si insert lanza exception`() = runTest {
        val repo = FakeWalletRepo(throwOnInsert = IllegalStateException("boom"))
        val uc = UpsertWalletUseCase(repo)

        val result = uc.execute(
            UpsertWalletCommand(
                portfolioId = 10,
                walletId = null,
                nameRaw = "Nueva",
                makeMain = false
            )
        )

        assertTrue(result is UpsertWalletResult.Failure)
        assertEquals("boom", (result as UpsertWalletResult.Failure).message)
        assertEquals(1, repo.insertCalls)
        assertEquals(0, repo.getByPortfolioCalls)
    }

    @Test
    fun `failure - si update lanza exception`() = runTest {
        val repo = FakeWalletRepo(
            initial = listOf(Wallet(walletId = 5, portfolioId = 10, name = "Old", description = "", isMain = false)),
            throwOnUpdateName = RuntimeException("update down")
        )
        val uc = UpsertWalletUseCase(repo)

        val result = uc.execute(
            UpsertWalletCommand(
                portfolioId = 10,
                walletId = 5,
                nameRaw = "Updated",
                makeMain = false
            )
        )

        assertTrue(result is UpsertWalletResult.Failure)
        assertEquals("update down", (result as UpsertWalletResult.Failure).message)
        assertEquals(1, repo.updateNameCalls)
        assertEquals(0, repo.getByPortfolioCalls)
    }

    @Test
    fun `failure - si getByPortfolio lanza exception`() = runTest {
        val repo = FakeWalletRepo(throwOnGetByPortfolio = RuntimeException("db down"))
        val uc = UpsertWalletUseCase(repo)

        val result = uc.execute(
            UpsertWalletCommand(
                portfolioId = 10,
                walletId = null,
                nameRaw = "Nueva",
                makeMain = false
            )
        )

        assertTrue(result is UpsertWalletResult.Failure)
        assertEquals("db down", (result as UpsertWalletResult.Failure).message)
        assertEquals(1, repo.insertCalls)
        assertEquals(1, repo.getByPortfolioCalls)
    }

    @Test
    fun `failure - mensaje nulo cae en Fallo desconocido`() = runTest {
        val repo = FakeWalletRepo(throwOnInsert = RuntimeException())
        val uc = UpsertWalletUseCase(repo)

        val result = uc.execute(
            UpsertWalletCommand(
                portfolioId = 10,
                walletId = null,
                nameRaw = "Nueva",
                makeMain = false
            )
        )

        assertTrue(result is UpsertWalletResult.Failure)
        assertEquals("Fallo desconocido", (result as UpsertWalletResult.Failure).message)
    }

    // ------------------------------------------------------------
    // Fake WalletRepository (compatible con WalletRepository.kt)
    // ------------------------------------------------------------
    private class FakeWalletRepo(
        initial: List<Wallet> = emptyList(),
        private val throwOnInsert: Throwable? = null,
        private val throwOnUpdateName: Throwable? = null,
        private val throwOnSetMain: Throwable? = null,
        private val throwOnGetByPortfolio: Throwable? = null
    ) : WalletRepository {

        private val itemsById = linkedMapOf<Long, Wallet>().apply {
            initial.forEach { put(it.walletId, it) }
        }

        var insertCalls = 0
        var updateNameCalls = 0
        var setMainCalls = 0
        var getByPortfolioCalls = 0

        var lastInserted: Wallet? = null
        var lastInsertedId: Long? = null

        var lastUpdateNameId: Long? = null
        var lastUpdateName: String? = null

        var lastSetMainId: Long? = null
        var lastGetByPortfolioId: Long? = null

        override suspend fun exists(walletId: Long): Boolean =
            itemsById.containsKey(walletId)

        override suspend fun belongsToPortfolio(walletId: Long, portfolioId: Long): Boolean =
            itemsById[walletId]?.portfolioId == portfolioId

        override suspend fun insert(wallet: Wallet): Long {
            insertCalls++
            lastInserted = wallet
            throwOnInsert?.let { throw it }

            val newId = (itemsById.keys.maxOrNull() ?: 0L) + 1L
            lastInsertedId = newId
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
            updateNameCalls++
            lastUpdateNameId = walletId
            lastUpdateName = name
            throwOnUpdateName?.let { throw it }

            val w = itemsById[walletId] ?: return
            itemsById[walletId] = w.copy(name = name)
        }
    }
}