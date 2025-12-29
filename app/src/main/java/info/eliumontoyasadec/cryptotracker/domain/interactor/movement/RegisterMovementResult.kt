package info.eliumontoyasadec.cryptotracker.domain.interactor.movement
data class RegisterMovementResult(
    val movementId: Long,
    val holdingId: String,
    val newHoldingQuantity: Double
)