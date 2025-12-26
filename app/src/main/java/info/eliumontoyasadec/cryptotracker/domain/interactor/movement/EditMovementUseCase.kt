package info.eliumontoyasadec.cryptotracker.domain.interactor.movement

import info.eliumontoyasadec.cryptotracker.domain.model.Movement
import info.eliumontoyasadec.cryptotracker.domain.model.MovementError
import info.eliumontoyasadec.cryptotracker.domain.repositories.HoldingRepository
import info.eliumontoyasadec.cryptotracker.domain.repositories.MovementRepository
 import info.eliumontoyasadec.cryptotracker.domain.repositories.TransactionRunner
import info.eliumontoyasadec.cryptotracker.domain.model.MovementType

class EditMovementUseCase(
    private val movementRepo: MovementRepository,
    private val holdingRepo: HoldingRepository,
    private val tx: TransactionRunner
) {

    suspend fun execute(cmd: EditMovementCommand): EditMovementResult = tx.runInTransaction {
        // 1) Validación básica
        if (cmd.movementId.isBlank()) throw MovementError.InvalidInput("movementId es requerido")
        if (cmd.newQuantity <= 0.0) throw MovementError.InvalidInput("quantity debe ser > 0")
        if (cmd.newTimestamp <= 0L) throw MovementError.InvalidInput("timestamp inválido")

        val newFee = cmd.newFeeQuantity ?: 0.0
        if (newFee < 0.0) throw MovementError.InvalidInput("feeQuantity no puede ser negativo")

        val requiresPrice = (cmd.newType == MovementType.BUY || cmd.newType == MovementType.SELL)
        if (requiresPrice) {
            val p = cmd.newPrice ?: throw MovementError.InvalidInput("price es requerido para BUY/SELL")
            if (p <= 0.0) throw MovementError.InvalidInput("price debe ser > 0")
        }

        // 2) Cargar movimiento existente
        val old = movementRepo.findById(cmd.movementId)
            ?: throw MovementError.NotFound("Movimiento no existe")

        // 3) Holding actual (mismo wallet+asset del movimiento)
        val holding = holdingRepo.findByWalletAsset(old.walletId, old.assetId)
        val currentQty = holding?.quantity ?: 0.0

        // 4) Deltas
        val deltaOld = computeDelta(old.type, old.quantity, old.feeQuantity)
        val deltaNew = computeDelta(cmd.newType, cmd.newQuantity, newFee)

        // 5) Ajuste neto
        val newHoldingQty = currentQty - deltaOld + deltaNew
        if (newHoldingQty < 0.0) {
            throw MovementError.InsufficientHoldings(
                "Holdings insuficientes al editar: actual=$currentQty, resultaría=$newHoldingQty"
            )
        }

        // 6) Persistir update de movimiento
        movementRepo.update(
            movementId = old.id,
            update = Movement(
                type = cmd.newType,
                quantity = cmd.newQuantity,
                price = cmd.newPrice,
                feeQuantity = newFee,
                timestamp = cmd.newTimestamp,
                notes = cmd.newNotes
            )
        )

        // 7) Upsert holding
        val updatedHolding = holdingRepo.upsert(
            portfolioId = old.portfolioId,
            walletId = old.walletId,
            assetId = old.assetId,
            newQuantity = newHoldingQty,
            updatedAt = System.currentTimeMillis()
        )

        EditMovementResult(
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