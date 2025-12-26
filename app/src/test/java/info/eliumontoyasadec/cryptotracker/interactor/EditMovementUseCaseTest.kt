package info.eliumontoyasadec.cryptotracker.interactor




import info.eliumontoyasadec.cryptotracker.domain.interactor.movement.EditMovementCommand
import info.eliumontoyasadec.cryptotracker.domain.interactor.movement.EditMovementUseCase
import info.eliumontoyasadec.cryptotracker.domain.model.MovementError
import info.eliumontoyasadec.cryptotracker.domain.model.MovementType
import info.eliumontoyasadec.cryptotracker.domain.model.Holding
import info.eliumontoyasadec.cryptotracker.domain.model.Movement
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Test
import kotlin.test.assertFailsWith



class EditMovementUseCaseTest {

    @Test
    fun `editar BUY ajusta holding correctamente`() = runTest {
        val movementRepo = FakeMovementRepo().apply {
            seed(
                Movement(
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

        val uc = EditMovementUseCase(
            movementRepo = movementRepo,
            holdingRepo = holdingRepo,
            tx = FakeTx()
        )

        val result = uc.execute(
            EditMovementCommand(
                movementId = "mov-1",
                newType = MovementType.BUY,
                newQuantity = 2.0,
                newPrice = 41000.0,
                newFeeQuantity = 0.0,
                newTimestamp = 1700000009999L,
                newNotes = "ajuste"
            )
        )

        assertEquals("mov-1", result.movementId)
        assertEquals("hol-1", result.holdingId)
        assertEquals(2.0, result.newHoldingQuantity, 0.0000001)

        // Verifica que se actualizó el movimiento
        assertEquals("mov-1", movementRepo.lastUpdateId)
         val updated = movementRepo.findById("mov-1")!!
        assertEquals(2.0, updated.quantity, 0.0)


        // Verifica holding upsert
        assertEquals(2.0, holdingRepo.lastUpsertQty!!, 0.0000001)
    }

    @Test
    fun `editar SELL falla si deja holding negativo y no persiste`() = runTest {
        val movementRepo = FakeMovementRepo().apply {
            seed(
                Movement(
                    id = "mov-2",
                    portfolioId = "p1",
                    walletId = "w1",
                    assetId = "btc",
                    type = MovementType.SELL,
                    quantity = 0.1,
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
                quantity = 0.2,
                updatedAt = 1L
            )
        }

        val uc = EditMovementUseCase(movementRepo, holdingRepo, FakeTx())


        assertFailsWith<MovementError.InsufficientHoldings> {
            uc.execute( EditMovementCommand(
                movementId = "mov-2",
                newType = MovementType.SELL,
                newQuantity = 0.5,
                newPrice = 46000.0,
                newFeeQuantity = 0.0,
                newTimestamp = 1700000010000L
            )
            )
        }



        // No update
        assertNull(movementRepo.lastUpdateId)
        assertNull(holdingRepo.lastUpsertQty)
        assertEquals(0.2, holdingRepo.holding!!.quantity, 0.0)
    }
}