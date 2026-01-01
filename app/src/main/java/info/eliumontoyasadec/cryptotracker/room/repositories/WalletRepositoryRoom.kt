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


    override suspend fun insert(wallet: Wallet): Long {
        // Insert primero para obtener ID real (Room autogenera)
        val entity = wallet.toEntity().copy(walletId = 0L)
        val newId = dao.insert(entity)

        // Si se insertó como main, ahora sí marca main con el ID real
        if (wallet.isMain) {
            setMain(newId)
        }
        return newId
    }

    override suspend fun update(wallet: Wallet) {
        val existing = dao.getById(wallet.walletId) ?: return

        // Actualiza campos normales (sin jugar con FK)
        val updated = existing.copy(
            portfolioId = wallet.portfolioId,
            name = wallet.name,
            description = wallet.description,
            isMain = wallet.isMain
        )
        dao.update(updated)

        // Si quedó como main, garantizar unicidad en ese portafolio
        if (wallet.isMain) {
            setMain(wallet.walletId)
        }
    }
    override suspend fun findById(walletId: Long): Wallet? =
        dao.getById(walletId)?.toDomain()

    override suspend fun getByPortfolio(portfolioId: Long): List<Wallet> =
        dao.getByPortfolio(portfolioId).map { it.toDomain() }


    override suspend fun delete(walletId: Long) {
        val existing = dao.getById(walletId) ?: return
        dao.delete(existing)
    }

    override suspend fun update(walletId: Long, name: String) {
        dao.updateName(walletId, name)
    }

    override suspend fun isMain(walletId: Long): Boolean =
        dao.isMain(walletId) ?: false

    override suspend fun setMain(walletId: Long) {
        val portfolioId = dao.portfolioIdOf(walletId)
            ?: throw IllegalArgumentException("Wallet $walletId no existe")

        // orden correcto para evitar inconsistencias
        dao.clearMainForPortfolio(portfolioId)
        dao.markMain(walletId)
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