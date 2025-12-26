package info.eliumontoyasadec.cryptotracker.interactor



import info.eliumontoyasadec.cryptotracker.domain.interactor.movement.DeleteMovementCommand
import info.eliumontoyasadec.cryptotracker.domain.interactor.movement.DeleteMovementUseCase
import info.eliumontoyasadec.cryptotracker.domain.model.MovementError
import info.eliumontoyasadec.cryptotracker.domain.model.MovementType
import info.eliumontoyasadec.cryptotracker.domain.model.Holding
import info.eliumontoyasadec.cryptotracker.domain.model.Movement
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Test
import kotlin.test.assertFailsWith




class DeleteMovementUseCaseTest {

    @Test
    fun `borrar BUY revierte holding y elimina movimiento`() = runTest {
        val movementRepo = FakeMovementRepo().apply {
            seed( Movement(
                id = "mov-1",
                portfolioId = "p1",
                walletId = "w1",
                assetId = "btc",
                type = MovementType.BUY,
                quantity = 1.0,
                price = 40000.0,
                feeQuantity = 0.0,
                timestamp = 1700000000000L,
                notes = null
            )
            )
        }

        val holdingRepo = FakeHoldingRepo().apply {
            holding = Holding(
                id = "hol-1",
                portfolioId = "p1",
                walletId = "w1",
                assetId = "btc",
                quantity = 1.0,
                updatedAt = 1L
            )
        }

        val uc = DeleteMovementUseCase(movementRepo, holdingRepo, FakeTx())

        val result = uc.execute(DeleteMovementCommand(movementId = "mov-1"))

        assertEquals("mov-1", result.movementId)
        assertEquals("hol-1", result.holdingId)
        assertEquals(0.0, result.newHoldingQuantity, 0.0000001)

        // holding actualizado
        assertEquals(0.0, holdingRepo.lastUpsertQty!!, 0.0000001)

        // movimiento eliminado
        assertEquals("mov-1", movementRepo.lastDeletedId)
        assertNull(movementRepo.findById("mov-1"))    }

    @Test
    fun `borrar SELL regresa cantidad al holding`() = runTest {
        val movementRepo = FakeMovementRepo().apply {
            seed(
                Movement(
                id = "mov-2",
                portfolioId = "p1",
                walletId = "w1",
                assetId = "btc",
                type = MovementType.SELL,
                quantity = 0.2,
                price = 45000.0,
                feeQuantity = 0.0,
                timestamp = 1700000000000L,
                notes = null
            )
            )
        }

        val holdingRepo = FakeHoldingRepo().apply {
            holding = Holding(
                id = "hol-2",
                portfolioId = "p1",
                walletId = "w1",
                assetId = "btc",
                quantity = 0.8,
                updatedAt = 1L
            )
        }

        val uc = DeleteMovementUseCase(movementRepo, holdingRepo, FakeTx())

        val result = uc.execute(DeleteMovementCommand("mov-2"))

        assertEquals(1.0, result.newHoldingQuantity, 0.0000001)
        assertEquals(1.0, holdingRepo.lastUpsertQty!!, 0.0000001)
        assertEquals("mov-2", movementRepo.lastDeletedId)
    }

    @Test
    fun `borrar falla si resultaria holding negativo y no elimina`() = runTest {
        val movementRepo = FakeMovementRepo().apply {
            seed( Movement(
                id = "mov-3",
                portfolioId = "p1",
                walletId = "w1",
                assetId = "btc",
                type = MovementType.BUY,
                quantity = 1.0,
                price = 40000.0,
                feeQuantity = 0.0,
                timestamp = 1700000000000L,
                notes = null
            )
            )
        }

        val holdingRepo = FakeHoldingRepo().apply {
            holding = Holding(
                id = "hol-3",
                portfolioId = "p1",
                walletId = "w1",
                assetId = "btc",
                quantity = 0.2, // inconsistente vs movimiento
                updatedAt = 1L
            )
        }

        val uc = DeleteMovementUseCase(movementRepo, holdingRepo, FakeTx())

        assertFailsWith<MovementError.InsufficientHoldings> {
            uc.execute(DeleteMovementCommand("mov-3"))
        }

        // No borró, no tocó holding
        assertNull(movementRepo.lastDeletedId)
        assertNull(holdingRepo.lastUpsertQty)
        assertNotNull(movementRepo.findById("mov-3"))     }

}