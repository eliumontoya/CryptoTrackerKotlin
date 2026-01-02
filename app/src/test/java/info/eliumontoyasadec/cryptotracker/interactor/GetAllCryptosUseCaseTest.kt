package info.eliumontoyasadec.cryptotracker.interactor.crypto

import info.eliumontoyasadec.cryptotracker.domain.interactor.crypto.GetAllCryptosUseCase
import info.eliumontoyasadec.cryptotracker.domain.model.Crypto
import info.eliumontoyasadec.cryptotracker.domain.repositories.CryptoRepository
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class GetAllCryptosUseCaseTest {

    @Test
    fun `execute devuelve lo que regresa el repo`() = runTest {
        val repo = FakeCryptoRepository(
            initial = listOf(
                Crypto(symbol = "BTC", name = "Bitcoin", coingeckoId = null, isActive = true),
                Crypto(symbol = "ETH", name = "Ethereum", coingeckoId = null, isActive = true)
            )
        )
        val uc = GetAllCryptosUseCase(repo)

        val result = uc.execute()

        assertEquals(2, result.size)
        assertEquals(listOf("BTC", "ETH"), result.map { it.symbol }.sorted())
        assertEquals(1, repo.getAllCalls)
    }

    // -------------------------
    // Fake repo para tests
    // -------------------------
    private class FakeCryptoRepository(
        initial: List<Crypto> = emptyList()
    ) : CryptoRepository {

        private val itemsBySymbol = linkedMapOf<String, Crypto>().apply {
            initial.forEach { put(it.symbol, it) }
        }

        var getAllCalls: Int = 0

        override suspend fun exists(assetId: String): Boolean =
            itemsBySymbol.containsKey(assetId)

        override suspend fun upsertAll(items: List<Crypto>) {
            items.forEach { itemsBySymbol[it.symbol] = it }
        }

        override suspend fun getAll(): List<Crypto> {
            getAllCalls++
            return itemsBySymbol.values.toList()
        }

        override suspend fun findBySymbol(symbol: String): Crypto? =
            itemsBySymbol[symbol]

        override suspend fun upsertOne(item: Crypto) {
            itemsBySymbol[item.symbol] = item
        }

        override suspend fun deleteBySymbol(symbol: String): Int {
            val existed = itemsBySymbol.remove(symbol) != null
            return if (existed) 1 else 0
        }
    }
}