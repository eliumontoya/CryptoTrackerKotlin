package info.eliumontoyasadec.cryptotracker.interactor


import info.eliumontoyasadec.cryptotracker.domain.interactor.movement.SwapMovementCommand
import info.eliumontoyasadec.cryptotracker.domain.interactor.movement.SwapMovementUseCase
import info.eliumontoyasadec.cryptotracker.domain.model.MovementError
import info.eliumontoyasadec.cryptotracker.domain.model.MovementType
import info.eliumontoyasadec.cryptotracker.domain.model.Holding
import info.eliumontoyasadec.cryptotracker.domain.model.Movement
import info.eliumontoyasadec.cryptotracker.domain.repositories.HoldingRepository
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Test
import kotlin.test.assertFailsWith
import info.eliumontoyasadec.cryptotracker.domain.repositories.*
import java.util.UUID



class SwapMovementUseCaseTest {

    @Test
    fun `swap exitoso crea 2 movimientos con mismo groupId y actualiza holdings de ambos assets`() = runTest {
        val movementRepo = FakeMovementRepoForSwap()
        val holdingRepo = FakeHoldingRepoMulti().apply {
            seed(Holding(id = "hol-btc", portfolioId = "p1", walletId = "w1", assetId = "btc", quantity = 1.0, updatedAt = 1L))
            seed(Holding(id = "hol-eth", portfolioId = "p1", walletId = "w1", assetId = "eth", quantity = 2.0, updatedAt = 1L))
        }

        val uc = SwapMovementUseCase(
            portfolioRepo = FakePortfolioRepo(exists = true),
            walletRepo = FakeWalletRepo(exists = true, belongs = true),
            assetRepo = FakeAssetRepo(exists = true),
            movementRepo = movementRepo,
            holdingRepo = holdingRepo,
            tx = FakeTx()
        )

        val cmd = SwapMovementCommand(
            portfolioId = "p1",
            walletId = "w1",
            fromAssetId = "btc",
            toAssetId = "eth",
            fromQuantity = 0.4,
            toQuantity = 1.5,
            timestamp = 1700000000000L,
            notes = "swap demo"
        )

        val result = uc.execute(cmd)

        // --- Movements (2 insertions) ---
        assertEquals(2, movementRepo.inserted.size)

        val sell = movementRepo.inserted.first { it.type == MovementType.SELL }
        val buy = movementRepo.inserted.first { it.type == MovementType.BUY }

        assertNotNull(sell.groupId)
        assertEquals(sell.groupId, buy.groupId)
        assertEquals(result.groupId, sell.groupId)

        assertEquals("p1", sell.portfolioId)
        assertEquals("w1", sell.walletId)
        assertEquals("btc", sell.assetId)
        assertEquals(0.4, sell.quantity, 0.0000001)

        assertEquals("p1", buy.portfolioId)
        assertEquals("w1", buy.walletId)
        assertEquals("eth", buy.assetId)
        assertEquals(1.5, buy.quantity, 0.0000001)

        // --- Holdings ---
        // BTC: 1.0 - 0.4 = 0.6
        // ETH: 2.0 + 1.5 = 3.5
        val btc = holdingRepo.findByWalletAsset("w1", "btc")!!
        val eth = holdingRepo.findByWalletAsset("w1", "eth")!!

        assertEquals(0.6, btc.quantity, 0.0000001)
        assertEquals(3.5, eth.quantity, 0.0000001)

        assertEquals(0.6, result.newFromHoldingQuantity, 0.0000001)
        assertEquals(3.5, result.newToHoldingQuantity, 0.0000001)

        // sanity: se hicieron 2 upserts (uno por asset)
        assertEquals(2, holdingRepo.upsertCalls)
    }

    @Test
    fun `swap exitoso cuando holding destino no existe lo crea desde 0`() = runTest {
        val movementRepo = FakeMovementRepoForSwap()
        val holdingRepo = FakeHoldingRepoMulti().apply {
            seed(Holding(id = "hol-btc", portfolioId = "p1", walletId = "w1", assetId = "btc", quantity = 1.0, updatedAt = 1L))
            // ETH NO existe
        }

        val uc = SwapMovementUseCase(
            portfolioRepo = FakePortfolioRepo(exists = true),
            walletRepo = FakeWalletRepo(exists = true, belongs = true),
            assetRepo = FakeAssetRepo(exists = true),
            movementRepo = movementRepo,
            holdingRepo = holdingRepo,
            tx = FakeTx()
        )

        val cmd = SwapMovementCommand(
            portfolioId = "p1",
            walletId = "w1",
            fromAssetId = "btc",
            toAssetId = "eth",
            fromQuantity = 0.25,
            toQuantity = 10.0,
            timestamp = 1700000000000L
        )

        val result = uc.execute(cmd)

        val btc = holdingRepo.findByWalletAsset("w1", "btc")!!
        val eth = holdingRepo.findByWalletAsset("w1", "eth")!!

        assertEquals(0.75, btc.quantity, 0.0000001)
        assertEquals(10.0, eth.quantity, 0.0000001)

        assertEquals("eth", eth.assetId)
        assertEquals(result.newToHoldingQuantity, eth.quantity, 0.0000001)
    }

    @Test
    fun `swap falla si holdings origen insuficientes y no inserta movimientos ni upserta holdings`() = runTest {
        val movementRepo = FakeMovementRepoForSwap()
        val holdingRepo = FakeHoldingRepoMulti().apply {
            seed(Holding(id = "hol-btc", portfolioId = "p1", walletId = "w1", assetId = "btc", quantity = 0.1, updatedAt = 1L))
        }

        val uc = SwapMovementUseCase(
            portfolioRepo = FakePortfolioRepo(exists = true),
            walletRepo = FakeWalletRepo(exists = true, belongs = true),
            assetRepo = FakeAssetRepo(exists = true),
            movementRepo = movementRepo,
            holdingRepo = holdingRepo,
            tx = FakeTx()
        )

        val cmd = SwapMovementCommand(
            portfolioId = "p1",
            walletId = "w1",
            fromAssetId = "btc",
            toAssetId = "eth",
            fromQuantity = 0.2, // > 0.1
            toQuantity = 1.0,
            timestamp = 1700000000000L
        )

        assertFailsWith<MovementError.InsufficientHoldings> {
            uc.execute(cmd)
        }

        assertEquals(0, movementRepo.inserted.size)
        assertEquals(0, holdingRepo.upsertCalls)

        // holding se mantiene igual
        assertEquals(0.1, holdingRepo.findByWalletAsset("w1", "btc")!!.quantity, 0.0)
    }

    @Test
    fun `swap falla si fromAssetId y toAssetId son iguales`() = runTest {
        val movementRepo = FakeMovementRepoForSwap()
        val holdingRepo = FakeHoldingRepoMulti()

        val uc = SwapMovementUseCase(
            portfolioRepo = FakePortfolioRepo(exists = true),
            walletRepo = FakeWalletRepo(exists = true, belongs = true),
            assetRepo = FakeAssetRepo(exists = true),
            movementRepo = movementRepo,
            holdingRepo = holdingRepo,
            tx = FakeTx()
        )

        val cmd = SwapMovementCommand(
            portfolioId = "p1",
            walletId = "w1",
            fromAssetId = "btc",
            toAssetId = "btc",
            fromQuantity = 0.1,
            toQuantity = 0.1,
            timestamp = 1700000000000L
        )

        assertFailsWith<MovementError.InvalidInput> {
            uc.execute(cmd)
        }

        assertEquals(0, movementRepo.inserted.size)
        assertEquals(0, holdingRepo.upsertCalls)
    }

    // -------------------------
    // Fakes locales (solo swap)
    // -------------------------

    private class FakeMovementRepoForSwap : MovementRepository {
        val inserted = mutableListOf<Movement>()

        override suspend fun insert(movement: Movement): String {
            val id = UUID.randomUUID().toString()
            val persisted = movement.copy(id = id)
            inserted += persisted
            return id
        }

        override suspend fun findById(movementId: String): Movement? = null
        override suspend fun update(movementId: String, update: Movement) = Unit
        override suspend fun delete(movementId: String) = Unit
    }

    private class FakeHoldingRepoMulti : HoldingRepository {
        private val store = mutableMapOf<String, Holding>()
        var upsertCalls: Int = 0
            private set

        fun seed(h: Holding) {
            store[key(h.walletId, h.assetId)] = h
        }

        override suspend fun findByWalletAsset(walletId: String, assetId: String): Holding? {
            return store[key(walletId, assetId)]
        }

        override suspend fun upsert(
            portfolioId: String,
            walletId: String,
            assetId: String,
            newQuantity: Double,
            updatedAt: Long
        ): Holding {
            upsertCalls++

            val k = key(walletId, assetId)
            val existing = store[k]
            val id = existing?.id ?: "hol-$walletId-$assetId"

            val newHolding = Holding(
                id = id,
                portfolioId = portfolioId,
                walletId = walletId,
                assetId = assetId,
                quantity = newQuantity,
                updatedAt = updatedAt
            )
            store[k] = newHolding
            return newHolding
        }

        private fun key(walletId: String, assetId: String) = "$walletId|$assetId"
    }
}