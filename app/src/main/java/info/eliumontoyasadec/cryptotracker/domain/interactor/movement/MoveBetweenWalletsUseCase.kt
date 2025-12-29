package info.eliumontoyasadec.cryptotracker.domain.interactor.movement

import info.eliumontoyasadec.cryptotracker.domain.model.Movement
import info.eliumontoyasadec.cryptotracker.domain.model.MovementError
import info.eliumontoyasadec.cryptotracker.domain.model.MovementType
import info.eliumontoyasadec.cryptotracker.domain.repositories.*
import java.util.UUID


/**
 * Mueve un asset entre 2 wallets dentro del mismo portafolio.
 *
 * Reglas de dominio:
 * - Operación atómica (transacción): si algo falla, no se persiste nada.
 * - Se registran 2 movimientos ligados por el mismo groupId:
 *   - TRANSFER_OUT en la wallet origen
 *   - TRANSFER_IN en la wallet destino
 * - Holdings:
 *   - Origen: -quantity
 *   - Destino: +quantity
 * - No se manejan fees (por ahora).
 *
 * Invariante clave:
 * - La suma de holdings del asset entre fromWalletId y toWalletId se mantiene constante
 *   (solo redistribución, no creación/destrucción).
 */
class MoveBetweenWalletsUseCase(
    private val portfolioRepo: PortfolioRepository,
    private val walletRepo: WalletRepository,
    private val assetRepo: AssetRepository,
    private val movementRepo: MovementRepository,
    private val holdingRepo: HoldingRepository,
    private val tx: TransactionRunner
) {

    suspend fun execute(cmd: MoveBetweenWalletsCommand): MoveBetweenWalletsResult =
        tx.runInTransaction {

            // 1) Validaciones básicas
            if (cmd.portfolioId == 0L) throw MovementError.InvalidInput("portfolioId requerido")
            if (cmd.fromWalletId == 0L) throw MovementError.InvalidInput("fromWalletId requerido")
            if (cmd.toWalletId == 0L) throw MovementError.InvalidInput("toWalletId requerido")
            if (cmd.fromWalletId == cmd.toWalletId)
                throw MovementError.InvalidInput("Las wallets deben ser distintas")

            if (cmd.assetId.isBlank()) throw MovementError.InvalidInput("assetId requerido")
            if (cmd.quantity <= 0.0) throw MovementError.InvalidInput("quantity debe ser > 0")
            if (cmd.timestamp <= 0L) throw MovementError.InvalidInput("timestamp inválido")

            // 2) Existencia
            if (!portfolioRepo.exists(cmd.portfolioId))
                throw MovementError.NotFound("Portafolio no existe")

            if (!walletRepo.exists(cmd.fromWalletId))
                throw MovementError.NotFound("Wallet origen no existe")

            if (!walletRepo.exists(cmd.toWalletId))
                throw MovementError.NotFound("Wallet destino no existe")

            if (!assetRepo.exists(cmd.assetId))
                throw MovementError.NotFound("Asset no existe")

            // 3) Pertenencia al portafolio
            if (!walletRepo.belongsToPortfolio(cmd.fromWalletId, cmd.portfolioId))
                throw MovementError.NotAllowed("Wallet origen no pertenece al portafolio")

            if (!walletRepo.belongsToPortfolio(cmd.toWalletId, cmd.portfolioId))
                throw MovementError.NotAllowed("Wallet destino no pertenece al portafolio")

            // 4) Holdings
            val fromHolding = holdingRepo.findByWalletAsset(cmd.fromWalletId, cmd.assetId)
            val currentFromQty = fromHolding?.quantity ?: 0.0
            val newFromQty = currentFromQty - cmd.quantity

            if (newFromQty < 0.0)
                throw MovementError.InsufficientHoldings(
                    "Holdings insuficientes: actual=$currentFromQty, requerido=${cmd.quantity}"
                )

            val toHolding = holdingRepo.findByWalletAsset(cmd.toWalletId, cmd.assetId)
            val currentToQty = toHolding?.quantity ?: 0.0
            val newToQty = currentToQty + cmd.quantity

            // 5) Persistencia atómica Todo: cambiar eesto a que la bd sea el que haga el groupid
            //val groupId = UUID.randomUUID().toString()
            val groupId = System.currentTimeMillis()

            val transferOutId = movementRepo.insert(
                Movement(
                    portfolioId = cmd.portfolioId,
                    walletId = cmd.fromWalletId,
                    assetId = cmd.assetId,
                    type = MovementType.TRANSFER_OUT,
                    quantity = cmd.quantity,
                    price = null,
                    feeQuantity = 0.0,
                    timestamp = cmd.timestamp,
                    notes = cmd.notes,
                    groupId = groupId
                )
            )

            val transferInId = movementRepo.insert(
                Movement(
                    portfolioId = cmd.portfolioId,
                    walletId = cmd.toWalletId,
                    assetId = cmd.assetId,
                    type = MovementType.TRANSFER_IN,
                    quantity = cmd.quantity,
                    price = null,
                    feeQuantity = 0.0,
                    timestamp = cmd.timestamp,
                    notes = cmd.notes,
                    groupId = groupId
                )
            )

            holdingRepo.upsert(
                portfolioId = cmd.portfolioId,
                walletId = cmd.fromWalletId,
                assetId = cmd.assetId,
                newQuantity = newFromQty,
                updatedAt = System.currentTimeMillis()
            )

            holdingRepo.upsert(
                portfolioId = cmd.portfolioId,
                walletId = cmd.toWalletId,
                assetId = cmd.assetId,
                newQuantity = newToQty,
                updatedAt = System.currentTimeMillis()
            )

            MoveBetweenWalletsResult(
                groupId = groupId,
                transferOutMovementId = transferOutId,
                transferInMovementId = transferInId,
                newFromHoldingQuantity = newFromQty,
                newToHoldingQuantity = newToQty
            )
        }
}