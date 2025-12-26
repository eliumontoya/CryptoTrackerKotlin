package info.eliumontoyasadec.cryptotracker.domain.interactor.movement

data class SwapMovementResult(
    val groupId: String,
    val sellMovementId: String,
    val buyMovementId: String,
    val fromHoldingId: String,
    val toHoldingId: String,
    val newFromHoldingQuantity: Double,
    val newToHoldingQuantity: Double
)