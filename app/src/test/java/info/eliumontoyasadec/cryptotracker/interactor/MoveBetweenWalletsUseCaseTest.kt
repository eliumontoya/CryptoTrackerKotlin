package info.eliumontoyasadec.cryptotracker.interactor

import info.eliumontoyasadec.cryptotracker.domain.model.Holding
import info.eliumontoyasadec.cryptotracker.domain.model.Movement
import info.eliumontoyasadec.cryptotracker.domain.model.MovementError
import info.eliumontoyasadec.cryptotracker.domain.model.MovementType
import info.eliumontoyasadec.cryptotracker.domain.repositories.*
import info.eliumontoyasadec.cryptotracker.domain.interactor.movement.*
import info.eliumontoyasadec.cryptotracker.domain.model.Crypto
import info.eliumontoyasadec.cryptotracker.domain.model.Portfolio
import info.eliumontoyasadec.cryptotracker.domain.model.Wallet


import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Test

class MoveBetweenWalletsUseCaseTest {

    @Test
    fun `mueve entre wallets y crea 2 movimientos con mismo groupId`() = runTest {
        val f = Fixture()
        f.portfolioRepo.portfolios += 1L
        f.walletRepo.walletToPortfolio[1L] = 1L
        f.walletRepo.walletToPortfolio[2L] = 1L
        f.assetRepo.assets += "btc"

        f.holdingRepo.putHolding("h1", 1L, 1L, "btc", 2.0)
        f.holdingRepo.putHolding("h2", 1L, 2L,  "btc", 5.0)

        val uc = f.useCase()

        val res = uc.execute(
            MoveBetweenWalletsCommand(
                portfolioId = 1L,
                fromWalletId = 1L,
                toWalletId = 2L,
                assetId = "btc",
                quantity = 0.75,
                timestamp = 1700000000000L,
                notes = "move test"
            )
        )

        assertEquals(2, f.movementRepo.inserted.size)

        val out = f.movementRepo.inserted[0]
        val inn = f.movementRepo.inserted[1]

        assertEquals(MovementType.TRANSFER_OUT, out.type)
        assertEquals(1L,  out.walletId)
        assertEquals("btc", out.assetId)
        assertEquals(0.75, out.quantity, 0.000001)
        assertEquals(res.groupId, out.groupId)

        assertEquals(MovementType.TRANSFER_IN, inn.type)
        assertEquals(2L,  inn.walletId)
        assertEquals("btc", inn.assetId)
        assertEquals(0.75, inn.quantity, 0.000001)
        assertEquals(res.groupId, inn.groupId)

        val fromH = f.holdingRepo.findByWalletAsset(1L,  "btc")!!
        val toH = f.holdingRepo.findByWalletAsset(2L,  "btc")!!

        assertEquals(1.25, fromH.quantity, 0.000001) // 2.0 - 0.75
        assertEquals(5.75, toH.quantity, 0.000001)   // 5.0 + 0.75
    }

    @Test
    fun `si el holding destino no existe lo crea con la cantidad recibida`() = runTest {
        val f = Fixture()
        f.portfolioRepo.portfolios += 1L
        f.walletRepo.walletToPortfolio[1L] = 1L
        f.walletRepo.walletToPortfolio[2L] = 1L
        f.assetRepo.assets += "btc"

        f.holdingRepo.putHolding("h1", 1L, 1L, "btc", 1.0)
        // w2 no tiene holding btc

        val uc = f.useCase()

        uc.execute(
            MoveBetweenWalletsCommand(
                portfolioId = 1L,
                fromWalletId = 1L,
                toWalletId = 2L,
                assetId = "btc",
                quantity = 0.4,
                timestamp = 1700000000000L
            )
        )

        val fromH = f.holdingRepo.findByWalletAsset(1L, "btc")!!
        val toH = f.holdingRepo.findByWalletAsset(2L, "btc")!!

        assertEquals(0.6, fromH.quantity, 0.000001)
        assertEquals(0.4, toH.quantity, 0.000001)
    }

    @Test
    fun `falla si holdings origen insuficientes y no inserta nada`() = runTest {
        val f = Fixture()
        f.portfolioRepo.portfolios += 1L
        f.walletRepo.walletToPortfolio[1L] = 1L
        f.walletRepo.walletToPortfolio[2L] = 1L
        f.assetRepo.assets += "btc"

        f.holdingRepo.putHolding("h1", 1L, 1L, "btc", 0.2)

        val uc = f.useCase()

        try {
            uc.execute(
                MoveBetweenWalletsCommand(
                    portfolioId = 1L,
                    fromWalletId = 1L,
                    toWalletId = 2L,
                    assetId = "btc",
                    quantity = 1.0,
                    timestamp = 1700000000000L
                )
            )
            fail("Debió lanzar InsufficientHoldings")
        } catch (e: MovementError.InsufficientHoldings) {
            // ok
        }

        assertEquals(0, f.movementRepo.inserted.size)
        // no debió tocar holdings destino
        assertNull(f.holdingRepo.findByWalletAsset(2L, "btc"))
        // holding origen intacto
        assertEquals(0.2, f.holdingRepo.findByWalletAsset(1L, "btc")!!.quantity, 0.000001)
    }

    @Test
    fun `invariante - la suma total del asset entre ambas wallets se mantiene`() = runTest {
        val f = Fixture()
        f.portfolioRepo.portfolios += 1L
        f.walletRepo.walletToPortfolio[1L] = 1L
        f.walletRepo.walletToPortfolio[2L] = 1L
        f.assetRepo.assets += "btc"

        f.holdingRepo.putHolding("h1", 1L, 1L, "btc", 3.0)
        f.holdingRepo.putHolding("h2", 1L, 2L, "btc", 7.0)



        val beforeSum = f.holdingRepo.findByWalletAsset(1L, "btc")!!.quantity +
                f.holdingRepo.findByWalletAsset(2L, "btc")!!.quantity

        val uc = f.useCase()

        uc.execute(
            MoveBetweenWalletsCommand(
                portfolioId = 1L,
                fromWalletId = 1L,
                toWalletId = 2L,
                assetId = "btc",
                quantity = 2.5,
                timestamp = 1700000000000L
            )
        )

        val afterSum = f.holdingRepo.findByWalletAsset(1L, "btc")!!.quantity +
                f.holdingRepo.findByWalletAsset(2L, "btc")!!.quantity

        assertEquals(beforeSum, afterSum, 0.000001)
    }

    // -----------------------------
    // Fixture + Fakes locales
    // -----------------------------

    private class Fixture {
        val portfolioRepo = FakePortfolioRepository()
        val walletRepo = FakeWalletRepository()
        val assetRepo = FakeCryptoRepository()
        val movementRepo = FakeMovementRepository()
        val holdingRepo = FakeHoldingRepository()
        val tx = FakeTransactionRunner()

        fun useCase() = MoveBetweenWalletsUseCase(
            portfolioRepo = portfolioRepo,
            walletRepo = walletRepo,
            assetRepo = assetRepo,
            movementRepo = movementRepo,
            holdingRepo = holdingRepo,
            tx = tx
        )
    }

    private class FakeTransactionRunner : TransactionRunner {
        override suspend fun <T> runInTransaction(block: suspend () -> T): T = block()
    }

    private class FakePortfolioRepository : PortfolioRepository {
        val portfolios = mutableSetOf<Long>()
        override suspend fun exists(portfolioId: Long): Boolean = portfolios.contains(portfolioId)
        override suspend fun insert(portfolio: Portfolio): Long {
            TODO("Not yet implemented")
        }

        override suspend fun findById(portfolioId: Long): Portfolio? {
            TODO("Not yet implemented")
        }

        override suspend fun getAll(): List<Portfolio> {
            TODO("Not yet implemented")
        }

        override suspend fun getDefault(): Portfolio? {
            TODO("Not yet implemented")
        }

        override suspend fun update(portfolio: Portfolio) {
            TODO("Not yet implemented")
        }

        override suspend fun delete(portfolioId: Long) {
            TODO("Not yet implemented")
        }

        override suspend fun delete(portfolio: Portfolio) {
            TODO("Not yet implemented")
        }

        override suspend fun isDefault(portfolioId: Long): Boolean {
            TODO("Not yet implemented")
        }

        override suspend fun setDefault(portfolioId: Long) {
            TODO("Not yet implemented")
        }
    }

    private class FakeWalletRepository : WalletRepository {
        val walletToPortfolio = mutableMapOf<Long, Long>()
        override suspend fun exists(walletId: Long): Boolean = walletToPortfolio.containsKey(walletId)
        override suspend fun belongsToPortfolio(walletId: Long, portfolioId: Long): Boolean =
            walletToPortfolio[walletId] == portfolioId

        override suspend fun insert(wallet: Wallet): Long {
            TODO("Not yet implemented")
        }

        override suspend fun findById(walletId: Long): Wallet? {
            TODO("Not yet implemented")
        }

        override suspend fun getByPortfolio(portfolioId: Long): List<Wallet> {
            TODO("Not yet implemented")
        }

        override suspend fun update(wallet: Wallet) {
            TODO("Not yet implemented")
        }

        override suspend fun update(walletId: Long, name: String) {
            TODO("Not yet implemented")
        }

        override suspend fun delete(walletId: Long) {
            TODO("Not yet implemented")
        }

        override suspend fun isMain(walletId: Long): Boolean {
            TODO("Not yet implemented")
        }

        override suspend fun setMain(walletId: Long) {
            TODO("Not yet implemented")
        }
    }

    private class FakeCryptoRepository : CryptoRepository {
        val assets = mutableSetOf<String>()
        override suspend fun exists(assetId: String): Boolean = assets.contains(assetId)
        override suspend fun upsertAll(items: List<Crypto>) {
            TODO("Not yet implemented")
        }

        override suspend fun getAll(): List<Crypto> {
            TODO("Not yet implemented")
        }

        override suspend fun findBySymbol(symbol: String): Crypto? {
            TODO("Not yet implemented")
        }

        override suspend fun upsertOne(item: Crypto) {
            TODO("Not yet implemented")
        }

        override suspend fun deleteBySymbol(symbol: String): Int {
            TODO("Not yet implemented")
        }
    }

    private class FakeMovementRepository : MovementRepository {

        private var seq = 0L
        private val store = linkedMapOf<Long, Movement>() // id -> Movement
        val inserted = mutableListOf<Movement>()            // para aserciones

        override suspend fun insert(movement: Movement): Long {
            seq += 1
            val id = seq
            val saved = movement.copy(id = id)
            store[id] = saved
            inserted += saved
            return id
        }

        override suspend fun findById(movementId: Long): Movement? {
            return store[movementId]
        }

        override suspend fun update(movementId: Long, update: Movement) {
            val current = store[movementId] ?: throw IllegalArgumentException("Movement no existe: $movementId")

            // update es un “parche” (sin ids/portfolio/wallet/asset). Actualizamos solo campos editables.
            val patched = current.copy(
                type = update.type,
                quantity = update.quantity,
                price = update.price,
                feeQuantity = update.feeQuantity,
                timestamp = update.timestamp,
                notes = update.notes,
                groupId = update.groupId ?: current.groupId
            )

            store[movementId] = patched
        }

        override suspend fun delete(movementId: Long) {
            store.remove(movementId)
        }
    }

    private class FakeHoldingRepository : HoldingRepository {
        private val map = mutableMapOf<String, Holding>() // key = "$walletId|$assetId"
        private var seq = 0

        fun putHolding(id: String, portfolioId: Long, walletId: Long, assetId: String, qty: Double) {
            map[key(walletId, assetId)] = Holding(
                id = id,
                portfolioId = portfolioId,
                walletId = walletId,
                assetId = assetId,
                quantity = qty,
                updatedAt = 0L
            )
        }

        override suspend fun findByWalletAsset(walletId: Long, assetId: String): Holding? =
            map[key(walletId, assetId)]

        override suspend fun upsert(
            portfolioId: Long,
            walletId: Long,
            assetId: String,
            newQuantity: Double,
            updatedAt: Long
        ): Holding {
            val k = key(walletId, assetId)
            val existing = map[k]
            val id = existing?.id ?: run {
                seq += 1
                "h-$seq"
            }

            val h = Holding(
                id = id,
                portfolioId = portfolioId,
                walletId = walletId,
                assetId = assetId,
                quantity = newQuantity,
                updatedAt = updatedAt
            )
            map[k] = h
            return h
        }

        private fun key(walletId: Long, assetId: String) = "$walletId|$assetId"
    }
}