package info.eliumontoyasadec.cryptotracker.interactor



import info.eliumontoyasadec.cryptotracker.domain.model.MovementType
import info.eliumontoyasadec.cryptotracker.domain.interactor.movement.RegisterMovementCommand
import info.eliumontoyasadec.cryptotracker.domain.model.MovementError
import info.eliumontoyasadec.cryptotracker.domain.interactor.movement.RegisterMovementUseCase
import info.eliumontoyasadec.cryptotracker.domain.model.Holding
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Test
import kotlin.test.assertFailsWith

class RegisterMovementUseCaseTest {

    @Test
    fun `BUY crea movimiento y actualiza holding incremental`() = runTest {
        val movementRepo = FakeMovementRepo()
        val holdingRepo = FakeHoldingRepo().apply { holding = null }

        val uc = RegisterMovementUseCase(
            portfolioRepo = FakePortfolioRepo(exists = true),
            walletRepo = FakeWalletRepo(exists = true, belongs = true),
            assetRepo = FakeAssetRepo(exists = true),
            movementRepo = movementRepo,
            holdingRepo = holdingRepo,
            tx = FakeTx()
        )

        val cmd = RegisterMovementCommand(
            portfolioId = "p1",
            walletId = "w1",
            assetId = "btc",
            type = MovementType.BUY,
            quantity = 0.5,
            price = 45000.0,
            feeQuantity = 0.001,
            timestamp = 1700000000000L,
            notes = null
        )

        val result = uc.execute(cmd)

        // --- Movement insert ---
        //assertEquals("mov-001", result.movementId)
        //assertNotNull(movementRepo.findById("mov-001"))
        assertNotNull(movementRepo.lastInserted)
        assertEquals("p1", movementRepo.lastInserted!!.portfolioId)
        assertEquals("w1", movementRepo.lastInserted!!.walletId)
        assertEquals("btc", movementRepo.lastInserted!!.assetId)
        assertEquals(MovementType.BUY, movementRepo.lastInserted!!.type)
        assertEquals(0.5, movementRepo.lastInserted!!.quantity, 0.0)
        assertEquals(0.001, movementRepo.lastInserted!!.feeQuantity, 0.0)

        // --- Holding upsert ---
        assertEquals("hol-001", result.holdingId)
        // 0.5 - 0.001 = 0.499
        assertEquals(0.499, result.newHoldingQuantity, 0.0000001)
        assertEquals(0.499, holdingRepo.lastUpsertQty!!, 0.0000001)
    }

    @Test
    fun `SELL falla si holdings insuficientes`() = runTest {
        val movementRepo = FakeMovementRepo()
        val holdingRepo = FakeHoldingRepo().apply {
            holding = Holding(
                id = "hol-777",
                portfolioId = "p1",
                walletId = "w1",
                assetId = "btc",
                quantity = 0.1,
                updatedAt = 1L
            )
        }

        val uc = RegisterMovementUseCase(
            portfolioRepo = FakePortfolioRepo(exists = true),
            walletRepo = FakeWalletRepo(exists = true, belongs = true),
            assetRepo = FakeAssetRepo(exists = true),
            movementRepo = movementRepo,
            holdingRepo = holdingRepo,
            tx = FakeTx()
        )

        val cmd = RegisterMovementCommand(
            portfolioId = "p1",
            walletId = "w1",
            assetId = "btc",
            type = MovementType.SELL,
            quantity = 0.2,           // quiere vender 0.2
            price = 45000.0,
            feeQuantity = 0.0,
            timestamp = 1700000000000L,
            notes = null
        )

        // IMPORTANTE: como execute() es suspend, usamos assertFailsWith dentro de runTest
        assertFailsWith<MovementError.InsufficientHoldings> {
            uc.execute(cmd)
        }

        // No debe insertar movimiento ni tocar holding
        assertNull(movementRepo.lastInserted)
        assertEquals(0.1, holdingRepo.holding!!.quantity, 0.0)
        assertNull(holdingRepo.lastUpsertQty)
    }
}
