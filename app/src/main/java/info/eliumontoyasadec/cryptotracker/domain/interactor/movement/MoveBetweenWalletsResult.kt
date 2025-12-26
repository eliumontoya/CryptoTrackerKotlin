package info.eliumontoyasadec.cryptotracker.domain.interactor.movement

data class MoveBetweenWalletsResult(
    val groupId: String,
    val transferOutMovementId: String,
    val transferInMovementId: String,
    val newFromHoldingQuantity: Double,
    val newToHoldingQuantity: Double
)