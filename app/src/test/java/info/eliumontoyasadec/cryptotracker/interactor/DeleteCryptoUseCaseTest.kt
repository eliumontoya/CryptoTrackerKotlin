package info.eliumontoyasadec.cryptotracker.interactor

import info.eliumontoyasadec.cryptotracker.domain.interactor.crypto.DeleteCryptoCommand
import info.eliumontoyasadec.cryptotracker.domain.interactor.crypto.DeleteCryptoResult
import info.eliumontoyasadec.cryptotracker.domain.interactor.crypto.DeleteCryptoUseCase
import info.eliumontoyasadec.cryptotracker.domain.model.Crypto
import info.eliumontoyasadec.cryptotracker.domain.repositories.CryptoRepository
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Test

class DeleteCryptoUseCaseTest {

    @Test
    fun `failure - symbol invalido regresa items via safeGetAll`() = runTest {
        val repo = FakeCryptoRepository(
            initial = listOf(
                Crypto(symbol = "BTC", name = "Bitcoin", coingeckoId = null, isActive = true)
            )
        )
        val uc = DeleteCryptoUseCase(repo)

        val result = uc.execute(DeleteCryptoCommand(symbolRaw = "   "))

        assertTrue(result is DeleteCryptoResult.Failure)
        val f = result as DeleteCryptoResult.Failure
        assertEquals("Símbolo inválido.", f.message)
        assertEquals(listOf("BTC"), f.items.map { it.symbol })
        assertEquals(0, repo.deleteCalls)
        assertEquals(1, repo.getAllCalls)
    }

    @Test
    fun `deleted - cuando deleteBySymbol devuelve mayor a 0`() = runTest {
        val repo = FakeCryptoRepository(
            initial = listOf(
                Crypto(symbol = "BTC", name = "Bitcoin", coingeckoId = null, isActive = true),
                Crypto(symbol = "ETH", name = "Ethereum", coingeckoId = null, isActive = true)
            )
        )
        val uc = DeleteCryptoUseCase(repo)

        val result = uc.execute(DeleteCryptoCommand(symbolRaw = "btc")) // lowercase a propósito

        assertTrue(result is DeleteCryptoResult.Deleted)
        val d = result as DeleteCryptoResult.Deleted

        // BTC eliminado, queda ETH
        assertEquals(listOf("ETH"), d.items.map { it.symbol })
        assertEquals(1, repo.deleteCalls)
        assertEquals("BTC", repo.lastDeleteSymbol) // normalizado a uppercase
        assertEquals(1, repo.getAllCalls)
    }

    @Test
    fun `not found - cuando deleteBySymbol devuelve 0`() = runTest {
        val repo = FakeCryptoRepository(
            initial = listOf(
                Crypto(symbol = "ETH", name = "Ethereum", coingeckoId = null, isActive = true)
            )
        )
        val uc = DeleteCryptoUseCase(repo)

        val result = uc.execute(DeleteCryptoCommand(symbolRaw = "btc"))

        assertTrue(result is DeleteCryptoResult.NotFound)
        val nf = result as DeleteCryptoResult.NotFound
        assertEquals(listOf("ETH"), nf.items.map { it.symbol })
        assertEquals(1, repo.deleteCalls)
        assertEquals("BTC", repo.lastDeleteSymbol)
        assertEquals(1, repo.getAllCalls)
    }

    @Test
    fun `in use - si delete lanza excepcion con mensaje`() = runTest {
        val repo = FakeCryptoRepository(
            initial = listOf(
                Crypto(symbol = "BTC", name = "Bitcoin", coingeckoId = null, isActive = true),
                Crypto(symbol = "ETH", name = "Ethereum", coingeckoId = null, isActive = true)
            ),
            throwOnDelete = IllegalStateException("FK constraint")
        )
        val uc = DeleteCryptoUseCase(repo)

        val result = uc.execute(DeleteCryptoCommand(symbolRaw = "btc"))

        assertTrue(result is DeleteCryptoResult.InUse)
        val iu = result as DeleteCryptoResult.InUse
        assertEquals("FK constraint", iu.message)
        // safeGetAll devuelve lo que hay (no borró nada)
        assertEquals(listOf("BTC", "ETH"), iu.items.map { it.symbol }.sorted())
        assertEquals(1, repo.deleteCalls)
        assertEquals(1, repo.getAllCalls)
    }

    @Test
    fun `in use - si delete lanza excepcion sin mensaje usa default`() = runTest {
        val repo = FakeCryptoRepository(
            initial = listOf(
                Crypto(symbol = "BTC", name = "Bitcoin", coingeckoId = null, isActive = true)
            ),
            throwOnDelete = RuntimeException()
        )
        val uc = DeleteCryptoUseCase(repo)

        val result = uc.execute(DeleteCryptoCommand(symbolRaw = "btc"))

        assertTrue(result is DeleteCryptoResult.InUse)
        val iu = result as DeleteCryptoResult.InUse
        assertEquals("No se pudo eliminar (posible relación con otros datos).", iu.message)
        assertEquals(listOf("BTC"), iu.items.map { it.symbol })
    }

    @Test
    fun `safeGetAll - si getAll falla, regresa lista vacia`() = runTest {
        val repo = FakeCryptoRepository(
            initial = listOf(
                Crypto(symbol = "BTC", name = "Bitcoin", coingeckoId = null, isActive = true)
            ),
            throwOnGetAll = RuntimeException("db down")
        )
        val uc = DeleteCryptoUseCase(repo)

        val result = uc.execute(DeleteCryptoCommand(symbolRaw = "   "))

        assertTrue(result is DeleteCryptoResult.Failure)
        val f = result as DeleteCryptoResult.Failure
        assertEquals(emptyList<Crypto>(), f.items)
    }

    // -------------------------
    // Fake repo para tests
    // -------------------------
    private class FakeCryptoRepository(
        initial: List<Crypto> = emptyList(),
        private val throwOnDelete: Throwable? = null,
        private val throwOnGetAll: Throwable? = null
    ) : CryptoRepository {

        private val itemsBySymbol = linkedMapOf<String, Crypto>().apply {
            initial.forEach { put(it.symbol, it) }
        }

        var deleteCalls: Int = 0
        var getAllCalls: Int = 0
        var lastDeleteSymbol: String? = null

        override suspend fun exists(assetId: String): Boolean =
            itemsBySymbol.containsKey(assetId)

        override suspend fun upsertAll(items: List<Crypto>) {
            items.forEach { itemsBySymbol[it.symbol] = it }
        }

        override suspend fun getAll(): List<Crypto> {
            getAllCalls++
            throwOnGetAll?.let { throw it }
            return itemsBySymbol.values.toList()
        }

        override suspend fun findBySymbol(symbol: String): Crypto? =
            itemsBySymbol[symbol]

        override suspend fun upsertOne(item: Crypto) {
            itemsBySymbol[item.symbol] = item
        }

        override suspend fun deleteBySymbol(symbol: String): Int {
            deleteCalls++
            lastDeleteSymbol = symbol
            throwOnDelete?.let { throw it }
            val existed = itemsBySymbol.remove(symbol) != null
            return if (existed) 1 else 0
        }
    }
}