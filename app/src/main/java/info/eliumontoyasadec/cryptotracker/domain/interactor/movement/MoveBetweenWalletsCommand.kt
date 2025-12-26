package info.eliumontoyasadec.cryptotracker.domain.interactor.movement

data class MoveBetweenWalletsCommand(
    val portfolioId: String,
    val fromWalletId: String,
    val toWalletId: String,
    val assetId: String,
    val quantity: Double,
    val timestamp: Long,
    val notes: String? = null
)