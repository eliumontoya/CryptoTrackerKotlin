package info.eliumontoyasadec.cryptotracker.e2e.fakes

import info.eliumontoyasadec.cryptotracker.domain.model.Crypto
import info.eliumontoyasadec.cryptotracker.domain.repositories.CryptoRepository

class FakeCryptoRepository : CryptoRepository {

    private val items = mutableListOf<Crypto>()

    fun seedDefaults() {
        upsertBlocking(
            listOf(
                Crypto(  symbol = "BTC", name = "Bitcoin"),
                Crypto(  symbol = "ETH", name = "Ethereum"),
                Crypto(  symbol = "SOL", name = "Solana")
            )
        )
    }

    override suspend fun exists(assetId: String): Boolean =
        items.any { it.symbol == assetId }

    override suspend fun upsertAll(items: List<Crypto>) {
        for (c in items) upsertOne(c)
    }

    override suspend fun getAll(): List<Crypto> =
        items.toList()

    override suspend fun findBySymbol(symbol: String): Crypto? =
        items.firstOrNull { it.symbol == symbol }

    override suspend fun upsertOne(item: Crypto) {
        val idx = items.indexOfFirst { it.symbol == item.symbol }
        if (idx >= 0) items[idx] = item else items.add(item)
    }

    override suspend fun deleteBySymbol(symbol: String): Int {
        val before = items.size
        items.removeAll { it.symbol == symbol }
        return before - items.size
    }

    // helper local para seedDefaults sin coroutine
    private fun upsertBlocking(list: List<Crypto>) {
        for (c in list) {
            val idx = items.indexOfFirst { it.symbol == c.symbol }
            if (idx >= 0) items[idx] = c else items.add(c)
        }
    }
}