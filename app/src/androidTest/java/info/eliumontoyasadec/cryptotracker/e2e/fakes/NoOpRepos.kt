package info.eliumontoyasadec.cryptotracker.e2e.fakes

import info.eliumontoyasadec.cryptotracker.domain.model.Crypto
import info.eliumontoyasadec.cryptotracker.domain.model.Fiat
import info.eliumontoyasadec.cryptotracker.domain.model.Holding
import info.eliumontoyasadec.cryptotracker.domain.model.Movement
import info.eliumontoyasadec.cryptotracker.domain.model.Wallet
import info.eliumontoyasadec.cryptotracker.domain.repositories.CryptoRepository
import info.eliumontoyasadec.cryptotracker.domain.repositories.FiatRepository
import info.eliumontoyasadec.cryptotracker.domain.repositories.HoldingRepository
import info.eliumontoyasadec.cryptotracker.domain.repositories.MovementRepository
import info.eliumontoyasadec.cryptotracker.domain.repositories.TransactionRunner
import info.eliumontoyasadec.cryptotracker.domain.repositories.WalletRepository
import info.eliumontoyasadec.cryptotracker.room.dao.HoldingDao
import info.eliumontoyasadec.cryptotracker.room.dao.MovementDao

class NoOpWalletRepository (
    initial: List<Wallet> = emptyList()
) : WalletRepository {
    override suspend fun exists(walletId: Long): Boolean = false
    override suspend fun belongsToPortfolio(walletId: Long, portfolioId: Long): Boolean {
        TODO("Not yet implemented")
    }

    override suspend fun insert(wallet: Wallet): Long = error("NoOpWalletRepository: not used in this E2E")
     override suspend fun findById(walletId: Long): Wallet? = null
    override suspend fun getByPortfolio(portfolioId: Long): List<Wallet> {
        TODO("Not yet implemented")
    }

    override suspend fun update(wallet: Wallet) {
        TODO("Not yet implemented")
    }

    override suspend fun update(walletId: Long, name: String) {
        TODO("Not yet implemented")
    }

    override suspend fun delete(walletId: Long) {}
    override suspend fun isMain(walletId: Long): Boolean {
        TODO("Not yet implemented")
    }

    override suspend fun setMain(walletId: Long) {
        TODO("Not yet implemented")
    }
}

class NoOpCryptoRepository : CryptoRepository {
    override suspend fun exists(assetId: String): Boolean = false
    override suspend fun upsertAll(items: List<Crypto>) = Unit
    override suspend fun getAll(): List<Crypto> = emptyList()
    override suspend fun findBySymbol(symbol: String): Crypto? = null
    override suspend fun upsertOne(item: Crypto) = Unit
    override suspend fun deleteBySymbol(symbol: String): Int = 0
}

class NoOpFiatRepository : FiatRepository {
    override suspend fun exists(code: String): Boolean = false
    override suspend fun upsertAll(items: List<Fiat>) = Unit
    override suspend fun getAll(): List<Fiat> = emptyList()
    override suspend fun findByCode(code: String): Fiat? = null
    override suspend fun countAll(): Int = 0
    override suspend fun upsert(item: Fiat) = Unit
    override suspend fun delete(code: String): Boolean = false
}

class NoOpMovementRepository(
 ) : MovementRepository {
    override suspend fun insert(movement: Movement): Long {
        TODO("Not yet implemented")
    }

    override suspend fun findById(movementId: Long): Movement? {
        TODO("Not yet implemented")
    }

    override suspend fun update(
        movementId: Long,
        update: Movement
    ) {
        TODO("Not yet implemented")
    }

    override suspend fun delete(movementId: Long) {
        TODO("Not yet implemented")
    }
}

class NoOpHoldingRepository(
 ) : HoldingRepository {
    override suspend fun findByWalletAsset(
        walletId: Long,
        assetId: String
    ): Holding? {
        TODO("Not yet implemented")
    }

    override suspend fun upsert(
        portfolioId: Long,
        walletId: Long,
        assetId: String,
        newQuantity: Double,
        updatedAt: Long
    ): Holding {
        TODO("Not yet implemented")
    }
}

class NoOpTransactionRunner(
) : TransactionRunner {
    override suspend fun <T> runInTransaction(block: suspend () -> T): T {
        TODO("Not yet implemented")
    }
}