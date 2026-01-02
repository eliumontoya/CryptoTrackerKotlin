package info.eliumontoyasadec.cryptotracker.interactor

import info.eliumontoyasadec.cryptotracker.domain.interactor.fiat.GetAllFiatsUseCase
import info.eliumontoyasadec.cryptotracker.domain.model.Fiat
import info.eliumontoyasadec.cryptotracker.domain.repositories.FiatRepository
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class GetAllFiatsUseCaseTest {

    @Test
    fun `execute devuelve lo que regresa el repo`() = runTest {
        val repo = FakeFiatRepo(
            initial = listOf(
                Fiat(code = "MXN", name = "Peso Mexicano", symbol = "$"),
                Fiat(code = "USD", name = "US Dollar", symbol = "US$")
            )
        )
        val uc = GetAllFiatsUseCase(repo)

        val result = uc.execute()

        assertEquals(2, result.size)
        assertEquals(listOf("MXN", "USD"), result.map { it.code }.sorted())
        assertEquals(1, repo.getAllCalls)
    }

    // ------------------------------------------------------------
    // Fake FiatRepository
    // ------------------------------------------------------------
    private class FakeFiatRepo(
        initial: List<Fiat> = emptyList(),
        private val throwOnGetAll: Throwable? = null
    ) : FiatRepository {

        private val itemsByCode = linkedMapOf<String, Fiat>().apply {
            initial.forEach { put(it.code, it) }
        }

        var getAllCalls = 0

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

        override suspend fun delete(code: String): Boolean =
            itemsByCode.remove(code) != null
    }
}