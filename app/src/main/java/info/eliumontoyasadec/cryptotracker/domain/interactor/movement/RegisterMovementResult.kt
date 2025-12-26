package info.eliumontoyasadec.cryptotracker.domain.interactor.movement
data class RegisterMovementResult(
    val movementId: String,
    val holdingId: String,
    val newHoldingQuantity: Double
)