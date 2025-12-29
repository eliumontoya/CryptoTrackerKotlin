package info.eliumontoyasadec.cryptotracker.domain.interactor.movement


import info.eliumontoyasadec.cryptotracker.domain.model.MovementError
import info.eliumontoyasadec.cryptotracker.domain.repositories.HoldingRepository
import info.eliumontoyasadec.cryptotracker.domain.repositories.MovementRepository
import info.eliumontoyasadec.cryptotracker.domain.repositories.TransactionRunner
import info.eliumontoyasadec.cryptotracker.domain.model.MovementType


class DeleteMovementUseCase(
    private val movementRepo: MovementRepository,
    private val holdingRepo: HoldingRepository,
    private val tx: TransactionRunner
) {

    suspend fun execute(cmd: DeleteMovementCommand): DeleteMovementResult = tx.runInTransaction {
        if (cmd.movementId == 0L) throw MovementError.InvalidInput("movementId es requerido")

        val old = movementRepo.findById(cmd.movementId)
            ?: throw MovementError.NotFound("Movimiento no existe")

        val holding = holdingRepo.findByWalletAsset(old.walletId, old.assetId)
        val currentQty = holding?.quantity ?: 0.0

        val deltaOld = computeDelta(old.type, old.quantity, old.feeQuantity)

        val newHoldingQty = currentQty - deltaOld
        if (newHoldingQty < 0.0) {
            throw MovementError.InsufficientHoldings(
                "Inconsistencia: al borrar dejaría holding negativo. actual=$currentQty, resultaría=$newHoldingQty"
            )
        }

        val updatedHolding = holdingRepo.upsert(
            portfolioId = old.portfolioId,
            walletId = old.walletId,
            assetId = old.assetId,
            newQuantity = newHoldingQty,
            updatedAt = System.currentTimeMillis()
        )

        movementRepo.delete(old.id)

        DeleteMovementResult(
            movementId = old.id,
            holdingId = updatedHolding.id,
            newHoldingQuantity = updatedHolding.quantity
        )
    }

    private fun computeDelta(type: MovementType, qty: Double, feeQty: Double): Double {
        val base = when (type) {
            MovementType.BUY, MovementType.DEPOSIT, MovementType.TRANSFER_IN -> +qty
            MovementType.SELL, MovementType.WITHDRAW, MovementType.TRANSFER_OUT, MovementType.FEE -> -qty
            MovementType.ADJUSTMENT-> qty

        }
        return base - feeQty
    }
}