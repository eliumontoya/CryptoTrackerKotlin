package info.eliumontoyasadec.cryptotracker.room.repositories

import info.eliumontoyasadec.cryptotracker.domain.model.Wallet
import info.eliumontoyasadec.cryptotracker.domain.repositories.WalletRepository
import info.eliumontoyasadec.cryptotracker.room.dao.WalletDao
import info.eliumontoyasadec.cryptotracker.room.entities.WalletEntity

class WalletRepositoryRoom(
    private val dao: WalletDao
) : WalletRepository {

    override suspend fun exists(walletId: Long): Boolean =
        dao.getById(walletId) != null

    override suspend fun belongsToPortfolio(walletId: Long, portfolioId: Long): Boolean =
        dao.getById(walletId)?.portfolioId == portfolioId

    override suspend fun insert(wallet: Wallet): Long =
        dao.insert(wallet.toEntity())

    override suspend fun findById(walletId: Long): Wallet? =
        dao.getById(walletId)?.toDomain()

    override suspend fun getByPortfolio(portfolioId: Long): List<Wallet> =
        dao.getByPortfolio(portfolioId).map { it.toDomain() }

    override suspend fun update(wallet: Wallet) {
        // Si prefieres “fallar” cuando no existe, aquí podrías lanzar error.
        // Por ahora, comportamiento seguro: si no existe, no hace nada.
        val existing = dao.getById(wallet.walletId) ?: return
        dao.update(
            existing.copy(
                portfolioId = wallet.portfolioId,
                name = wallet.name,
                description = wallet.description,
                isMain = wallet.isMain
            )
        )
    }

    override suspend fun delete(walletId: Long) {
        val existing = dao.getById(walletId) ?: return
        dao.delete(existing)
    }
}

/* =======================
   MAPPERS (estilo MovementRepositoryRoom)
   ======================= */

private fun WalletEntity.toDomain(): Wallet = Wallet(
    walletId = walletId,
    portfolioId = portfolioId,
    name = name,
    description = description,
    isMain = isMain
)

private fun Wallet.toEntity(): WalletEntity = WalletEntity(
    walletId = walletId,
    portfolioId = portfolioId,
    name = name,
    description = description,
    isMain = isMain
)