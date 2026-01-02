package info.eliumontoyasadec.cryptotracker.interactor

import info.eliumontoyasadec.cryptotracker.domain.interactor.fiat.DeleteFiatCommand
import info.eliumontoyasadec.cryptotracker.domain.interactor.fiat.DeleteFiatResult
import info.eliumontoyasadec.cryptotracker.domain.interactor.fiat.DeleteFiatUseCase
import info.eliumontoyasadec.cryptotracker.domain.model.Fiat
import info.eliumontoyasadec.cryptotracker.domain.repositories.FiatRepository
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Test

class DeleteFiatUseCaseTest {

    @Test
    fun `failure - code invalido regresa items via safeGetAll`() = runTest {
        val repo = FakeFiatRepo(
            initial = listOf(
                Fiat(code = "MXN", name = "Peso Mexicano", symbol = "$")
            )
        )
        val uc = DeleteFiatUseCase(repo)

        val result = uc.execute(DeleteFiatCommand(codeRaw = "   "))

        assertTrue(result is DeleteFiatResult.Failure)
        val f = result as DeleteFiatResult.Failure
        assertEquals("Código inválido.", f.message)
        assertEquals(listOf("MXN"), f.items.map { it.code })
        assertEquals(0, repo.deleteCalls)
        assertEquals(1, repo.getAllCalls)
    }

    @Test
    fun `deleted - cuando repo delete regresa true`() = runTest {
        val repo = FakeFiatRepo(
            initial = listOf(
                Fiat(code = "MXN", name = "Peso Mexicano", symbol = "$"),
                Fiat(code = "USD", name = "US Dollar", symbol = "US$")
            )
        )
        val uc = DeleteFiatUseCase(repo)

        val result = uc.execute(DeleteFiatCommand(codeRaw = "  mxn "))

        assertTrue(result is DeleteFiatResult.Deleted)
        val d = result as DeleteFiatResult.Deleted

        assertEquals(listOf("USD"), d.items.map { it.code })
        assertEquals(1, repo.deleteCalls)
        assertEquals("MXN", repo.lastDeleteCode) // uppercase
        assertEquals(1, repo.getAllCalls)
    }

    @Test
    fun `not found - cuando repo delete regresa false`() = runTest {
        val repo = FakeFiatRepo(
            initial = listOf(
                Fiat(code = "USD", name = "US Dollar", symbol = "US$")
            )
        )
        val uc = DeleteFiatUseCase(repo)

        val result = uc.execute(DeleteFiatCommand(codeRaw = "mxn"))

        assertTrue(result is DeleteFiatResult.NotFound)
        val nf = result as DeleteFiatResult.NotFound

        assertEquals(listOf("USD"), nf.items.map { it.code })
        assertEquals(1, repo.deleteCalls)
        assertEquals("MXN", repo.lastDeleteCode)
        assertEquals(1, repo.getAllCalls)
    }

    @Test
    fun `failure - si repo delete lanza excepcion, regresa safeGetAll`() = runTest {
        val repo = FakeFiatRepo(
            initial = listOf(
                Fiat(code = "MXN", name = "Peso Mexicano", symbol = "$"),
                Fiat(code = "USD", name = "US Dollar", symbol = "US$")
            ),
            throwOnDelete = IllegalStateException("FK constraint")
        )
        val uc = DeleteFiatUseCase(repo)

        val result = uc.execute(DeleteFiatCommand(codeRaw = "mxn"))

        assertTrue(result is DeleteFiatResult.Failure)
        val f = result as DeleteFiatResult.Failure

        assertEquals("FK constraint", f.message)
        assertEquals(listOf("MXN", "USD"), f.items.map { it.code }.sorted())
        assertEquals(1, repo.deleteCalls)
        assertEquals(1, repo.getAllCalls) // safeGetAll
    }

    @Test
    fun `safeGetAll - si getAll falla, regresa lista vacia`() = runTest {
        val repo = FakeFiatRepo(
            initial = listOf(Fiat(code = "MXN", name = "Peso Mexicano", symbol = "$")),
            throwOnGetAll = RuntimeException("db down")
        )
        val uc = DeleteFiatUseCase(repo)

        val result = uc.execute(DeleteFiatCommand(codeRaw = "   "))

        assertTrue(result is DeleteFiatResult.Failure)
        val f = result as DeleteFiatResult.Failure

        assertEquals(emptyList<Fiat>(), f.items)
        assertEquals(0, repo.deleteCalls)
        assertEquals(1, repo.getAllCalls) // safeGetAll intentó
    }

    // ------------------------------------------------------------
    // Fake FiatRepository
    // ------------------------------------------------------------
    private class FakeFiatRepo(
        initial: List<Fiat> = emptyList(),
        private val throwOnDelete: Throwable? = null,
        private val throwOnGetAll: Throwable? = null
    ) : FiatRepository {

        private val itemsByCode = linkedMapOf<String, Fiat>().apply {
            initial.forEach { put(it.code, it) }
        }

        var deleteCalls = 0
        var getAllCalls = 0
        var lastDeleteCode: String? = null

        override suspend fun exists(code: String): Boolean =
            itemsByCode.containsKey(code)

        override suspend fun upsertAll(items: List<Fiat>) {
            items.forEach { itemsByCode[it.code] = it }
        }

        override suspend fun getAll(): List<Fiat> {
            getAllCalls++
            throwOnGetAll?.let { throw it }
            return itemsByCode.values.toList()
        }

        override suspend fun findByCode(code: String): Fiat? =
            itemsByCode[code]

        override suspend fun countAll(): Int =
            itemsByCode.size

        override suspend fun upsert(item: Fiat) {
            itemsByCode[item.code] = item
        }

        override suspend fun delete(code: String): Boolean {
            deleteCalls++
            lastDeleteCode = code
            throwOnDelete?.let { throw it }
            return itemsByCode.remove(code) != null
        }
    }
}