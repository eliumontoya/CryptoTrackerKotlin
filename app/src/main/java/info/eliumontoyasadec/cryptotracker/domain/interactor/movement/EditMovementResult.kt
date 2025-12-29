package info.eliumontoyasadec.cryptotracker.domain.interactor.movement

data class EditMovementResult(
    val movementId: Long,
    val holdingId: String,
    val newHoldingQuantity: Double
)