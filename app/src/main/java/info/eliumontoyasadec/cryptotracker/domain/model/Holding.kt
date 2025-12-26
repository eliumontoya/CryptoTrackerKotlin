package info.eliumontoyasadec.cryptotracker.domain.model
data class Holding(
    val id: String,
    val portfolioId: String,
    val walletId: String,
    val assetId: String,
    val quantity: Double,
    val updatedAt: Long
)