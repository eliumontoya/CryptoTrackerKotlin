package info.eliumontoyasadec.cryptotracker.domain.interactor.movement

import info.eliumontoyasadec.cryptotracker.domain.model.Movement
import info.eliumontoyasadec.cryptotracker.domain.model.MovementError
import info.eliumontoyasadec.cryptotracker.domain.model.MovementType
import info.eliumontoyasadec.cryptotracker.domain.repositories.*
import java.util.UUID

class SwapMovementUseCase(
    private val portfolioRepo: PortfolioRepository,
    private val walletRepo: WalletRepository,
    private val assetRepo: AssetRepository,
    private val movementRepo: MovementRepository,
    private val holdingRepo: HoldingRepository,
    private val tx: TransactionRunner
) {
    suspend fun execute(cmd: SwapMovementCommand): SwapMovementResult = tx.runInTransaction {
        // 1) Validación de entrada
        if (cmd.portfolioId == 0L) throw MovementError.InvalidInput("portfolioId es requerido")
        if (cmd.walletId == 0L) throw MovementError.InvalidInput("walletId es requerido")
        if (cmd.fromAssetId.isBlank()) throw MovementError.InvalidInput("fromAssetId es requerido")
        if (cmd.toAssetId.isBlank()) throw MovementError.InvalidInput("toAssetId es requerido")
        if (cmd.fromAssetId == cmd.toAssetId) throw MovementError.InvalidInput("fromAssetId y toAssetId deben ser distintos")
        if (cmd.fromQuantity <= 0.0) throw MovementError.InvalidInput("fromQuantity debe ser > 0")
        if (cmd.toQuantity <= 0.0) throw MovementError.InvalidInput("toQuantity debe ser > 0")
        if (cmd.timestamp <= 0L) throw MovementError.InvalidInput("timestamp inválido")

        // 2) Existencia y pertenencia
        if (!portfolioRepo.exists(cmd.portfolioId)) throw MovementError.NotFound("Portafolio no existe")
        if (!walletRepo.exists(cmd.walletId)) throw MovementError.NotFound("Wallet no existe")
        if (!assetRepo.exists(cmd.fromAssetId)) throw MovementError.NotFound("Asset origen no existe")
        if (!assetRepo.exists(cmd.toAssetId)) throw MovementError.NotFound("Asset destino no existe")
        if (!walletRepo.belongsToPortfolio(cmd.walletId, cmd.portfolioId)) {
            throw MovementError.NotAllowed("La wallet no pertenece al portafolio")
        }

        // 3) Validación holdings del asset origen
        val fromHolding = holdingRepo.findByWalletAsset(cmd.walletId, cmd.fromAssetId)
        val currentFromQty = fromHolding?.quantity ?: 0.0
        val newFromQty = currentFromQty - cmd.fromQuantity
        if (newFromQty < 0.0) {
            throw MovementError.InsufficientHoldings(
                "Holdings insuficientes (origen): actual=$currentFromQty, requerido=${cmd.fromQuantity}"
            )
        }

        // 4) Holding destino (puede no existir)
        val toHolding = holdingRepo.findByWalletAsset(cmd.walletId, cmd.toAssetId)
        val currentToQty = toHolding?.quantity ?: 0.0
        val newToQty = currentToQty + cmd.toQuantity

        // 5) Persistencia atómica: 2 movimientos + 2 upserts
        //  Todo: cambiar eesto a que la bd sea el que haga el groupid
        //val groupId = UUID.randomUUID().toString()
        val groupId = System.currentTimeMillis()

        val sellMovementId = movementRepo.insert(
            Movement(
                portfolioId = cmd.portfolioId,
                walletId = cmd.walletId,
                assetId = cmd.fromAssetId,
                type = MovementType.SELL,
                quantity = cmd.fromQuantity,
                price = null,
                feeQuantity = 0.0,
                timestamp = cmd.timestamp,
                notes = cmd.notes,
                groupId = groupId
            )
        )

        val buyMovementId = movementRepo.insert(
            Movement(
                portfolioId = cmd.portfolioId,
                walletId = cmd.walletId,
                assetId = cmd.toAssetId,
                type = MovementType.BUY,
                quantity = cmd.toQuantity,
                price = null,
                feeQuantity = 0.0,
                timestamp = cmd.timestamp,
                notes = cmd.notes,
                groupId = groupId
            )
        )

        val upsertFrom = holdingRepo.upsert(
            portfolioId = cmd.portfolioId,
            walletId = cmd.walletId,
            assetId = cmd.fromAssetId,
            newQuantity = newFromQty,
            updatedAt = System.currentTimeMillis()
        )

        val upsertTo = holdingRepo.upsert(
            portfolioId = cmd.portfolioId,
            walletId = cmd.walletId,
            assetId = cmd.toAssetId,
            newQuantity = newToQty,
            updatedAt = System.currentTimeMillis()
        )

        SwapMovementResult(
            groupId = groupId,
            sellMovementId = sellMovementId,
            buyMovementId = buyMovementId,
            fromHoldingId = upsertFrom.id,
            toHoldingId = upsertTo.id,
            newFromHoldingQuantity = upsertFrom.quantity,
            newToHoldingQuantity = upsertTo.quantity
        )
    }
}