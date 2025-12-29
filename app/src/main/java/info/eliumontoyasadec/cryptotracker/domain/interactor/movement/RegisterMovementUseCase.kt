package info.eliumontoyasadec.cryptotracker.domain.interactor.movement
import info.eliumontoyasadec.cryptotracker.domain.model.Movement
import info.eliumontoyasadec.cryptotracker.domain.model.MovementError
import info.eliumontoyasadec.cryptotracker.domain.repositories.CryptoRepository
import info.eliumontoyasadec.cryptotracker.domain.repositories.HoldingRepository
import info.eliumontoyasadec.cryptotracker.domain.repositories.MovementRepository
 import info.eliumontoyasadec.cryptotracker.domain.repositories.PortfolioRepository
import info.eliumontoyasadec.cryptotracker.domain.repositories.TransactionRunner
import info.eliumontoyasadec.cryptotracker.domain.repositories.WalletRepository
import info.eliumontoyasadec.cryptotracker.domain.model.MovementType

class RegisterMovementUseCase(
    private val portfolioRepo: PortfolioRepository,
    private val walletRepo: WalletRepository,
    private val assetRepo: CryptoRepository,
    private val movementRepo: MovementRepository,
    private val holdingRepo: HoldingRepository,
    private val tx: TransactionRunner
) {

    suspend fun execute(cmd: RegisterMovementCommand): RegisterMovementResult = tx.runInTransaction {
        // 1) Validación de entrada
        if (cmd.portfolioId == 0L) throw MovementError.InvalidInput("portfolioId es requerido")
        if (cmd.walletId == 0L) throw MovementError.InvalidInput("walletId es requerido")
        if (cmd.assetId.isBlank()) throw MovementError.InvalidInput("assetId es requerido")
        if (cmd.quantity <= 0.0) throw MovementError.InvalidInput("quantity debe ser > 0")
        if (cmd.timestamp <= 0L) throw MovementError.InvalidInput("timestamp inválido")

        val fee = cmd.feeQuantity ?: 0.0
        if (fee < 0.0) throw MovementError.InvalidInput("feeQuantity no puede ser negativo")

        val requiresPrice = (cmd.type == MovementType.BUY || cmd.type == MovementType.SELL)
        if (requiresPrice) {
            val p = cmd.price ?: throw MovementError.InvalidInput("price es requerido para BUY/SELL")
            if (p <= 0.0) throw MovementError.InvalidInput("price debe ser > 0")
        }

        // 2) Normalización mínima (fee null -> 0 ya se hizo)
        val normalizedQty = cmd.quantity
        val normalizedFee = fee

        // 3) Existencia y pertenencia
        if (!portfolioRepo.exists(cmd.portfolioId)) throw MovementError.NotFound("Portafolio no existe")
        if (!walletRepo.exists(cmd.walletId)) throw MovementError.NotFound("Wallet no existe")
        if (!assetRepo.exists(cmd.assetId)) throw MovementError.NotFound("Asset no existe")
        if (!walletRepo.belongsToPortfolio(cmd.walletId, cmd.portfolioId)) {
            throw MovementError.NotAllowed("La wallet no pertenece al portafolio")
        }

        // 4) Validación de saldo no negativo (si resta)
        val currentHolding = holdingRepo.findByWalletAsset(cmd.walletId, cmd.assetId)
        val currentQty = currentHolding?.quantity ?: 0.0
        val deltaQty: Double = (when (cmd.type) {
            MovementType.BUY, MovementType.DEPOSIT, MovementType.TRANSFER_IN -> +normalizedQty
            MovementType.SELL, MovementType.WITHDRAW, MovementType.TRANSFER_OUT, MovementType.FEE -> -normalizedQty
            else -> 0.0
        }) - normalizedFee

        val newQty = currentQty + deltaQty
        if (newQty < 0.0) {
            throw MovementError.InsufficientHoldings(
                "Holdings insuficientes: actual=$currentQty, requerido=${-deltaQty}"
            )
        }

        // 5) Persistir movimiento
        val movementId = movementRepo.insert(
            Movement(
                portfolioId = cmd.portfolioId,
                walletId = cmd.walletId,
                assetId = cmd.assetId,
                type = cmd.type,
                quantity = normalizedQty,
                price = cmd.price,
                feeQuantity = normalizedFee,
                timestamp = cmd.timestamp,
                notes = cmd.notes
            )
        )

        // 6) Upsert holding
        val holding = holdingRepo.upsert(
            portfolioId = cmd.portfolioId,
            walletId = cmd.walletId,
            assetId = cmd.assetId,
            newQuantity = newQty,
            updatedAt = System.currentTimeMillis()
        )

        // 7) Resultado
        RegisterMovementResult(
            movementId = movementId,
            holdingId = holding.id,
            newHoldingQuantity = holding.quantity
        )
    }
}