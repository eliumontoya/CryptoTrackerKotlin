package info.eliumontoyasadec.cryptotracker.domain.interactor.movement

data class MoveBetweenWalletsResult(
    val groupId: Long,
    val transferOutMovementId: Long,
    val transferInMovementId: Long,
    val newFromHoldingQuantity: Double,
    val newToHoldingQuantity: Double
)