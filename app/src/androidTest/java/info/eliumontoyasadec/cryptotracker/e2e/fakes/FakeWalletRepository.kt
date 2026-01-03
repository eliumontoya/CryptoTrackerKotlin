package info.eliumontoyasadec.cryptotracker.e2e.fakes

import info.eliumontoyasadec.cryptotracker.domain.model.Wallet
import info.eliumontoyasadec.cryptotracker.domain.repositories.WalletRepository

class FakeWalletRepository(
    seed: List<Wallet> = emptyList()
) : WalletRepository {

    private val items = seed.toMutableList()
    private var nextId: Long = (items.maxOfOrNull { it.walletId } ?: 0L) + 1L

    override suspend fun exists(walletId: Long): Boolean =
        items.any { it.walletId == walletId }

    override suspend fun belongsToPortfolio(walletId: Long, portfolioId: Long): Boolean =
        items.firstOrNull { it.walletId == walletId }?.portfolioId == portfolioId

    override suspend fun insert(wallet: Wallet): Long {
        val id = if (wallet.walletId > 0) wallet.walletId else nextId++
        val toInsert = wallet.copy(walletId = id)

        // Si ya existe, lo reemplazamos; si no, lo agregamos.
        val idx = items.indexOfFirst { it.walletId == id }
        if (idx >= 0) items[idx] = toInsert else items.add(toInsert)

        // Si se insertó como principal, respetamos una sola principal por portafolio.
        if (toInsert.isMain) {
            setMain(id)
        }

        return id
    }

    override suspend fun findById(walletId: Long): Wallet? =
        items.firstOrNull { it.walletId == walletId }

    override suspend fun getByPortfolio(portfolioId: Long): List<Wallet> =
        items.filter { it.portfolioId == portfolioId }
            .sortedBy { it.walletId }

    override suspend fun update(wallet: Wallet) {
        val idx = items.indexOfFirst { it.walletId == wallet.walletId }
        if (idx >= 0) {
            items[idx] = wallet
            if (wallet.isMain) setMain(wallet.walletId)
        }
    }

    override suspend fun update(walletId: Long, name: String) {
        val idx = items.indexOfFirst { it.walletId == walletId }
        if (idx >= 0) {
            items[idx] = items[idx].copy(name = name)
        }
    }

    override suspend fun delete(walletId: Long) {
        items.removeAll { it.walletId == walletId }
    }

    override suspend fun isMain(walletId: Long): Boolean =
        items.firstOrNull { it.walletId == walletId }?.isMain == true

    override suspend fun setMain(walletId: Long) {
        val target = items.firstOrNull { it.walletId == walletId } ?: return
        val pid = target.portfolioId

        for (i in items.indices) {
            val w = items[i]
            if (w.portfolioId == pid) {
                items[i] = w.copy(isMain = (w.walletId == walletId))
            }
        }
    }
}