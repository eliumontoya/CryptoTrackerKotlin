package info.eliumontoyasadec.cryptotracker.domain.interactor.movement

data class MoveBetweenWalletsCommand(
    val portfolioId: Long,
    val fromWalletId: Long,
    val toWalletId: Long,
    val assetId: String,
    val quantity: Double,
    val timestamp: Long,
    val notes: String = ""
)