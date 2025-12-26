package info.eliumontoyasadec.cryptotracker.domain.interactor

import info.eliumontoyasadec.cryptotracker.domain.model.Holding
import info.eliumontoyasadec.cryptotracker.domain.model.Movement
import info.eliumontoyasadec.cryptotracker.domain.model.MovementError
import info.eliumontoyasadec.cryptotracker.domain.model.MovementType
import info.eliumontoyasadec.cryptotracker.domain.repositories.*
import info.eliumontoyasadec.cryptotracker.domain.interactor.movement.*


import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Test

class MoveBetweenWalletsUseCaseTest {

    @Test
    fun `mueve entre wallets y crea 2 movimientos con mismo groupId`() = runTest {
        val f = Fixture()
        f.portfolioRepo.portfolios += "p1"
        f.walletRepo.walletToPortfolio["w1"] = "p1"
        f.walletRepo.walletToPortfolio["w2"] = "p1"
        f.assetRepo.assets += "btc"

        f.holdingRepo.putHolding("h1", "p1", "w1", "btc", 2.0)
        f.holdingRepo.putHolding("h2", "p1", "w2", "btc", 5.0)

        val uc = f.useCase()

        val res = uc.execute(
            MoveBetweenWalletsCommand(
                portfolioId = "p1",
                fromWalletId = "w1",
                toWalletId = "w2",
                assetId = "btc",
                quantity = 0.75,
                timestamp = 1700000000000L,
                notes = "move test"
            )
        )

        assertTrue(res.groupId.isNotBlank())
        assertEquals(2, f.movementRepo.inserted.size)

        val out = f.movementRepo.inserted[0]
        val inn = f.movementRepo.inserted[1]

        assertEquals(MovementType.TRANSFER_OUT, out.type)
        assertEquals("w1", out.walletId)
        assertEquals("btc", out.assetId)
        assertEquals(0.75, out.quantity, 0.000001)
        assertEquals(res.groupId, out.groupId)

        assertEquals(MovementType.TRANSFER_IN, inn.type)
        assertEquals("w2", inn.walletId)
        assertEquals("btc", inn.assetId)
        assertEquals(0.75, inn.quantity, 0.000001)
        assertEquals(res.groupId, inn.groupId)

        val fromH = f.holdingRepo.findByWalletAsset("w1", "btc")!!
        val toH = f.holdingRepo.findByWalletAsset("w2", "btc")!!

        assertEquals(1.25, fromH.quantity, 0.000001) // 2.0 - 0.75
        assertEquals(5.75, toH.quantity, 0.000001)   // 5.0 + 0.75
    }

    @Test
    fun `si el holding destino no existe lo crea con la cantidad recibida`() = runTest {
        val f = Fixture()
        f.portfolioRepo.portfolios += "p1"
        f.walletRepo.walletToPortfolio["w1"] = "p1"
        f.walletRepo.walletToPortfolio["w2"] = "p1"
        f.assetRepo.assets += "btc"

        f.holdingRepo.putHolding("h1", "p1", "w1", "btc", 1.0)
        // w2 no tiene holding btc

        val uc = f.useCase()

        uc.execute(
            MoveBetweenWalletsCommand(
                portfolioId = "p1",
                fromWalletId = "w1",
                toWalletId = "w2",
                assetId = "btc",
                quantity = 0.4,
                timestamp = 1700000000000L
            )
        )

        val fromH = f.holdingRepo.findByWalletAsset("w1", "btc")!!
        val toH = f.holdingRepo.findByWalletAsset("w2", "btc")!!

        assertEquals(0.6, fromH.quantity, 0.000001)
        assertEquals(0.4, toH.quantity, 0.000001)
    }

    @Test
    fun `falla si holdings origen insuficientes y no inserta nada`() = runTest {
        val f = Fixture()
        f.portfolioRepo.portfolios += "p1"
        f.walletRepo.walletToPortfolio["w1"] = "p1"
        f.walletRepo.walletToPortfolio["w2"] = "p1"
        f.assetRepo.assets += "btc"

        f.holdingRepo.putHolding("h1", "p1", "w1", "btc", 0.2)

        val uc = f.useCase()

        try {
            uc.execute(
                MoveBetweenWalletsCommand(
                    portfolioId = "p1",
                    fromWalletId = "w1",
                    toWalletId = "w2",
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
        assertNull(f.holdingRepo.findByWalletAsset("w2", "btc"))
        // holding origen intacto
        assertEquals(0.2, f.holdingRepo.findByWalletAsset("w1", "btc")!!.quantity, 0.000001)
    }

    @Test
    fun `invariante - la suma total del asset entre ambas wallets se mantiene`() = runTest {
        val f = Fixture()
        f.portfolioRepo.portfolios += "p1"
        f.walletRepo.walletToPortfolio["w1"] = "p1"
        f.walletRepo.walletToPortfolio["w2"] = "p1"
        f.assetRepo.assets += "btc"

        f.holdingRepo.putHolding("h1", "p1", "w1", "btc", 3.0)
        f.holdingRepo.putHolding("h2", "p1", "w2", "btc", 7.0)

        val beforeSum = f.holdingRepo.findByWalletAsset("w1", "btc")!!.quantity +
                f.holdingRepo.findByWalletAsset("w2", "btc")!!.quantity

        val uc = f.useCase()

        uc.execute(
            MoveBetweenWalletsCommand(
                portfolioId = "p1",
                fromWalletId = "w1",
                toWalletId = "w2",
                assetId = "btc",
                quantity = 2.5,
                timestamp = 1700000000000L
            )
        )

        val afterSum = f.holdingRepo.findByWalletAsset("w1", "btc")!!.quantity +
                f.holdingRepo.findByWalletAsset("w2", "btc")!!.quantity

        assertEquals(beforeSum, afterSum, 0.000001)
    }

    // -----------------------------
    // Fixture + Fakes locales
    // -----------------------------

    private class Fixture {
        val portfolioRepo = FakePortfolioRepository()
        val walletRepo = FakeWalletRepository()
        val assetRepo = FakeAssetRepository()
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
        val portfolios = mutableSetOf<String>()
        override suspend fun exists(portfolioId: String): Boolean = portfolios.contains(portfolioId)
    }

    private class FakeWalletRepository : WalletRepository {
        val walletToPortfolio = mutableMapOf<String, String>()
        override suspend fun exists(walletId: String): Boolean = walletToPortfolio.containsKey(walletId)
        override suspend fun belongsToPortfolio(walletId: String, portfolioId: String): Boolean =
            walletToPortfolio[walletId] == portfolioId
    }

    private class FakeAssetRepository : AssetRepository {
        val assets = mutableSetOf<String>()
        override suspend fun exists(assetId: String): Boolean = assets.contains(assetId)
    }

    private class FakeMovementRepository : MovementRepository {

        private var seq = 0
        private val store = linkedMapOf<String, Movement>() // id -> Movement
        val inserted = mutableListOf<Movement>()            // para aserciones

        override suspend fun insert(movement: Movement): String {
            seq += 1
            val id = "mov-$seq"
            val saved = movement.copy(id = id)
            store[id] = saved
            inserted += saved
            return id
        }

        override suspend fun findById(movementId: String): Movement? {
            return store[movementId]
        }

        override suspend fun update(movementId: String, update: Movement) {
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

        override suspend fun delete(movementId: String) {
            store.remove(movementId)
        }
    }

    private class FakeHoldingRepository : HoldingRepository {
        private val map = mutableMapOf<String, Holding>() // key = "$walletId|$assetId"
        private var seq = 0

        fun putHolding(id: String, portfolioId: String, walletId: String, assetId: String, qty: Double) {
            map[key(walletId, assetId)] = Holding(
                id = id,
                portfolioId = portfolioId,
                walletId = walletId,
                assetId = assetId,
                quantity = qty,
                updatedAt = 0L
            )
        }

        override suspend fun findByWalletAsset(walletId: String, assetId: String): Holding? =
            map[key(walletId, assetId)]

        override suspend fun upsert(
            portfolioId: String,
            walletId: String,
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

        private fun key(walletId: String, assetId: String) = "$walletId|$assetId"
    }
}