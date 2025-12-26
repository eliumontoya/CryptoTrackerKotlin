package info.eliumontoyasadec.cryptotracker.domain.interactor.movement

data class SwapMovementCommand(
    val portfolioId: String,
    val walletId: String,
    val fromAssetId: String,
    val toAssetId: String,
    val fromQuantity: Double,
    val toQuantity: Double,
    val timestamp: Long,
    val notes: String? = null
)