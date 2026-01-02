package info.eliumontoyasadec.cryptotracker.interactor.crypto

import info.eliumontoyasadec.cryptotracker.domain.interactor.crypto.UpsertCryptoCommand
import info.eliumontoyasadec.cryptotracker.domain.interactor.crypto.UpsertCryptoResult
import info.eliumontoyasadec.cryptotracker.domain.interactor.crypto.UpsertCryptoUseCase
import info.eliumontoyasadec.cryptotracker.domain.model.Crypto
import info.eliumontoyasadec.cryptotracker.domain.repositories.CryptoRepository
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Test

class UpsertCryptoUseCaseTest {

    @Test
    fun `validation error si symbol esta vacio`() = runTest {
        val repo = FakeCryptoRepository()
        val uc = UpsertCryptoUseCase(repo)

        val result = uc.execute(
            UpsertCryptoCommand(
                symbolRaw = "   ",
                nameRaw = "Bitcoin",
                isActive = true,
                isEditing = false
            )
        )

        assertTrue(result is UpsertCryptoResult.ValidationError)
        assertEquals("El símbolo es obligatorio.", (result as UpsertCryptoResult.ValidationError).message)
        assertEquals(0, repo.upsertOneCalls)
    }

    @Test
    fun `validation error si name esta vacio`() = runTest {
        val repo = FakeCryptoRepository()
        val uc = UpsertCryptoUseCase(repo)

        val result = uc.execute(
            UpsertCryptoCommand(
                symbolRaw = "btc",
                nameRaw = "   ",
                isActive = true,
                isEditing = false
            )
        )

        assertTrue(result is UpsertCryptoResult.ValidationError)
        assertEquals("El nombre es obligatorio.", (result as UpsertCryptoResult.ValidationError).message)
        assertEquals(0, repo.upsertOneCalls)
    }

    @Test
    fun `success - normaliza symbol a uppercase y trimea name`() = runTest {
        val repo = FakeCryptoRepository(
            initial = listOf(
                Crypto(symbol = "ETH", name = "Ethereum", coingeckoId = null, isActive = true)
            )
        )
        val uc = UpsertCryptoUseCase(repo)

        val result = uc.execute(
            UpsertCryptoCommand(
                symbolRaw = "  btc ",
                nameRaw = "  Bitcoin  ",
                isActive = true,
                isEditing = false
            )
        )

        assertTrue(result is UpsertCryptoResult.Success)
        val s = result as UpsertCryptoResult.Success
        assertFalse(s.wasUpdate)

        // Se guardó como BTC
        assertEquals("BTC", repo.lastUpsert?.symbol)
        assertEquals("Bitcoin", repo.lastUpsert?.name)
        assertEquals(true, repo.lastUpsert?.isActive)

        // Y la lista final incluye ETH + BTC
        val symbols = s.items.map { it.symbol }.sorted()
        assertEquals(listOf("BTC", "ETH"), symbols)
        assertEquals(1, repo.upsertOneCalls)
        assertEquals(1, repo.getAllCalls)
    }

    @Test
    fun `success - wasUpdate refleja cmd_isEditing`() = runTest {
        val repo = FakeCryptoRepository()
        val uc = UpsertCryptoUseCase(repo)

        val result = uc.execute(
            UpsertCryptoCommand(
                symbolRaw = "btc",
                nameRaw = "Bitcoin",
                isActive = true,
                isEditing = true
            )
        )

        assertTrue(result is UpsertCryptoResult.Success)
        assertTrue((result as UpsertCryptoResult.Success).wasUpdate)
    }

    @Test
    fun `failure - si repo upsert lanza excepcion`() = runTest {
        val repo = FakeCryptoRepository(throwOnUpsert = RuntimeException("boom"))
        val uc = UpsertCryptoUseCase(repo)

        val result = uc.execute(
            UpsertCryptoCommand(
                symbolRaw = "btc",
                nameRaw = "Bitcoin",
                isActive = true,
                isEditing = false
            )
        )

        assertTrue(result is UpsertCryptoResult.Failure)
        assertEquals("boom", (result as UpsertCryptoResult.Failure).message)
        assertEquals(1, repo.upsertOneCalls) // intentó upsert
    }

    // -------------------------
    // Fake repo para tests
    // -------------------------
    private class FakeCryptoRepository(
        initial: List<Crypto> = emptyList(),
        private val throwOnUpsert: Throwable? = null
    ) : CryptoRepository {

        private val itemsBySymbol = linkedMapOf<String, Crypto>().apply {
            initial.forEach { put(it.symbol, it) }
        }

        var upsertOneCalls: Int = 0
        var getAllCalls: Int = 0
        var lastUpsert: Crypto? = null

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
            upsertOneCalls++
            lastUpsert = item
            throwOnUpsert?.let { throw it }
            itemsBySymbol[item.symbol] = item
        }

        override suspend fun deleteBySymbol(symbol: String): Int {
            val existed = itemsBySymbol.remove(symbol) != null
            return if (existed) 1 else 0
        }
    }
}