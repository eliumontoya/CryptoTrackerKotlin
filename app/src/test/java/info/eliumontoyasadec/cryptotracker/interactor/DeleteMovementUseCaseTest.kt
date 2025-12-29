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
                id = 1L,
                portfolioId = 1L,
                walletId = 1L,
                assetId = "btc",
                type = MovementType.BUY,
                quantity = 1.0,
                price = 40000.0,
                feeQuantity = 0.0,
                timestamp = 1700000000000L,
                notes = ""
            )
            )
        }

        val holdingRepo = FakeHoldingRepo().apply {
            holding = Holding(
                id = "hol-1",
                portfolioId = 1L,
                walletId = 1L,
                assetId = "btc",
                quantity = 1.0,
                updatedAt = 1L
            )
        }

        val uc = DeleteMovementUseCase(movementRepo, holdingRepo, FakeTx())

        val result = uc.execute(DeleteMovementCommand(movementId = 1L))

        assertEquals(1L, result.movementId)
        assertEquals("hol-1", result.holdingId)
        assertEquals(0.0, result.newHoldingQuantity, 0.0000001)

        // holding actualizado
        assertEquals(0.0, holdingRepo.lastUpsertQty!!, 0.0000001)

        // movimiento eliminado
        assertEquals(1L, movementRepo.lastDeletedId)
        assertNull(movementRepo.findById(1L))    }

    @Test
    fun `borrar SELL regresa cantidad al holding`() = runTest {
        val movementRepo = FakeMovementRepo().apply {
            seed(
                Movement(
                id = 2L,
                portfolioId = 1L,
                walletId = 1L,
                assetId = "btc",
                type = MovementType.SELL,
                quantity = 0.2,
                price = 45000.0,
                feeQuantity = 0.0,
                timestamp = 1700000000000L,
                notes = ""
            )
            )
        }

        val holdingRepo = FakeHoldingRepo().apply {
            holding = Holding(
                id = "hol-2",
                portfolioId = 1L,
                walletId = 1L,
                assetId = "btc",
                quantity = 0.8,
                updatedAt = 1L
            )
        }

        val uc = DeleteMovementUseCase(movementRepo, holdingRepo, FakeTx())

        val result = uc.execute(DeleteMovementCommand(2L))

        assertEquals(1.0, result.newHoldingQuantity, 0.0000001)
        assertEquals(1.0, holdingRepo.lastUpsertQty!!, 0.0000001)
        assertEquals(2L, movementRepo.lastDeletedId)
    }

    @Test
    fun `borrar falla si resultaria holding negativo y no elimina`() = runTest {
        val movementRepo = FakeMovementRepo().apply {
            seed( Movement(
                id = 3L,
                portfolioId = 1L,
                walletId = 1L,
                assetId = "btc",
                type = MovementType.BUY,
                quantity = 1.0,
                price = 40000.0,
                feeQuantity = 0.0,
                timestamp = 1700000000000L,
                notes = ""
            )
            )
        }

        val holdingRepo = FakeHoldingRepo().apply {
            holding = Holding(
                id = "hol-3",
                portfolioId = 1L,
                walletId = 1L,
                assetId = "btc",
                quantity = 0.2, // inconsistente vs movimiento
                updatedAt = 1L
            )
        }

        val uc = DeleteMovementUseCase(movementRepo, holdingRepo, FakeTx())

        assertFailsWith<MovementError.InsufficientHoldings> {
            uc.execute(DeleteMovementCommand(3L))
        }

        // No borró, no tocó holding
        assertNull(movementRepo.lastDeletedId)
        assertNull(holdingRepo.lastUpsertQty)
        assertNotNull(movementRepo.findById(3L))     }

}