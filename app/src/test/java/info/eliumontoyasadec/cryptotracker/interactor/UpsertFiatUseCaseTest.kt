package info.eliumontoyasadec.cryptotracker.interactor

import info.eliumontoyasadec.cryptotracker.domain.interactor.fiat.UpsertFiatCommand
import info.eliumontoyasadec.cryptotracker.domain.interactor.fiat.UpsertFiatResult
import info.eliumontoyasadec.cryptotracker.domain.interactor.fiat.UpsertFiatUseCase
import info.eliumontoyasadec.cryptotracker.domain.model.Fiat
import info.eliumontoyasadec.cryptotracker.domain.repositories.FiatRepository
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Test

class UpsertFiatUseCaseTest {

    @Test
    fun `validation error si code esta vacio`() = runTest {
        val repo = FakeFiatRepo()
        val uc = UpsertFiatUseCase(repo)

        val result = uc.execute(
            UpsertFiatCommand(
                codeRaw = "   ",
                nameRaw = "Peso Mexicano",
                symbolRaw = "$",
                isEditing = false
            )
        )

        assertTrue(result is UpsertFiatResult.ValidationError)
        assertEquals("El código no puede estar vacío", (result as UpsertFiatResult.ValidationError).message)
        assertEquals(0, repo.upsertCalls)
        assertEquals(0, repo.getAllCalls)
    }

    @Test
    fun `validation error si name esta vacio`() = runTest {
        val repo = FakeFiatRepo()
        val uc = UpsertFiatUseCase(repo)

        val result = uc.execute(
            UpsertFiatCommand(
                codeRaw = "mxn",
                nameRaw = "   ",
                symbolRaw = "$",
                isEditing = false
            )
        )

        assertTrue(result is UpsertFiatResult.ValidationError)
        assertEquals("El nombre no puede estar vacío", (result as UpsertFiatResult.ValidationError).message)
        assertEquals(0, repo.upsertCalls)
        assertEquals(0, repo.getAllCalls)
    }

    @Test
    fun `success - normaliza code uppercase y trimea name y symbol`() = runTest {
        val repo = FakeFiatRepo(
            initial = listOf(Fiat(code = "USD", name = "US Dollar", symbol = "US$"))
        )
        val uc = UpsertFiatUseCase(repo)

        val result = uc.execute(
            UpsertFiatCommand(
                codeRaw = "  mxn ",
                nameRaw = "  Peso Mexicano  ",
                symbolRaw = "  $  ",
                isEditing = false
            )
        )

        assertTrue(result is UpsertFiatResult.Success)
        val s = result as UpsertFiatResult.Success

        assertFalse(s.wasUpdate)
        assertEquals(1, repo.upsertCalls)
        assertEquals(1, repo.getAllCalls)

        assertEquals("MXN", repo.lastUpsert?.code)
        assertEquals("Peso Mexicano", repo.lastUpsert?.name)
        assertEquals("$", repo.lastUpsert?.symbol)

        assertEquals(listOf("MXN", "USD"), s.items.map { it.code }.sorted())
    }

    @Test
    fun `success - symbolRaw null se guarda como string vacio`() = runTest {
        val repo = FakeFiatRepo()
        val uc = UpsertFiatUseCase(repo)

        val result = uc.execute(
            UpsertFiatCommand(
                codeRaw = "mxn",
                nameRaw = "Peso Mexicano",
                symbolRaw = null,
                isEditing = true
            )
        )

        assertTrue(result is UpsertFiatResult.Success)
        val s = result as UpsertFiatResult.Success

        assertTrue(s.wasUpdate)
        assertEquals("", repo.lastUpsert?.symbol)
    }

    @Test
    fun `failure - si repo upsert lanza excepcion`() = runTest {
        val repo = FakeFiatRepo(throwOnUpsert = RuntimeException("boom"))
        val uc = UpsertFiatUseCase(repo)

        val result = uc.execute(
            UpsertFiatCommand(
                codeRaw = "mxn",
                nameRaw = "Peso Mexicano",
                symbolRaw = "$",
                isEditing = false
            )
        )

        assertTrue(result is UpsertFiatResult.Failure)
        assertEquals("boom", (result as UpsertFiatResult.Failure).message)
        assertEquals(1, repo.upsertCalls) // intentó
        assertEquals(0, repo.getAllCalls) // no llegó
    }

    @Test
    fun `failure - si repo getAll lanza excepcion despues del upsert`() = runTest {
        val repo = FakeFiatRepo(throwOnGetAll = RuntimeException("db down"))
        val uc = UpsertFiatUseCase(repo)

        val result = uc.execute(
            UpsertFiatCommand(
                codeRaw = "mxn",
                nameRaw = "Peso Mexicano",
                symbolRaw = "$",
                isEditing = false
            )
        )

        assertTrue(result is UpsertFiatResult.Failure)
        assertEquals("db down", (result as UpsertFiatResult.Failure).message)
        assertEquals(1, repo.upsertCalls)
        assertEquals(1, repo.getAllCalls)
    }

    // ------------------------------------------------------------
    // Fake FiatRepository
    // ------------------------------------------------------------
    private class FakeFiatRepo(
        initial: List<Fiat> = emptyList(),
        private val throwOnUpsert: Throwable? = null,
        private val throwOnGetAll: Throwable? = null
    ) : FiatRepository {

        private val itemsByCode = linkedMapOf<String, Fiat>().apply {
            initial.forEach { put(it.code, it) }
        }

        var upsertCalls = 0
        var getAllCalls = 0
        var lastUpsert: Fiat? = null

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
            upsertCalls++
            lastUpsert = item
            throwOnUpsert?.let { throw it }
            itemsByCode[item.code] = item
        }

        override suspend fun delete(code: String): Boolean =
            itemsByCode.remove(code) != null
    }
}