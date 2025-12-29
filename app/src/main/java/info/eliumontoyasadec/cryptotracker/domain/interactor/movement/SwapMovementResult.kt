package info.eliumontoyasadec.cryptotracker.domain.interactor.movement

data class SwapMovementResult(
    val groupId: Long,
    val sellMovementId: Long,
    val buyMovementId: Long,
    val fromHoldingId: String,
    val toHoldingId: String,
    val newFromHoldingQuantity: Double,
    val newToHoldingQuantity: Double
)