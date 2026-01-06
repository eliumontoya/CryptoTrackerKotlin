package info.eliumontoyasadec.cryptotracker.e2e.fakes

import info.eliumontoyasadec.cryptotracker.domain.model.Fiat
import info.eliumontoyasadec.cryptotracker.domain.repositories.FiatRepository

class FakeFiatRepository(
    seed: List<Fiat> = emptyList()
) : FiatRepository {

    private val items = seed.associateBy { it.code }.toMutableMap()

    override suspend fun exists(code: String): Boolean = items.containsKey(code)

    override suspend fun upsertAll(items: List<Fiat>) {
        items.forEach { this.items[it.code] = it }
    }

    override suspend fun getAll(): List<Fiat> = items.values.sortedBy { it.code }

    override suspend fun findByCode(code: String): Fiat? = items[code]

    override suspend fun countAll(): Int = items.size

    override suspend fun upsert(item: Fiat) {
        items[item.code] = item
    }

    override suspend fun delete(code: String): Boolean = (items.remove(code) != null)
}