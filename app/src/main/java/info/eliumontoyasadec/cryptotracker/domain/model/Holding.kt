package info.eliumontoyasadec.cryptotracker.domain.model
data class Holding(
    val id: String,
    val portfolioId: Long,
    val walletId: Long,
    val assetId: String,
    val quantity: Double,
    val updatedAt: Long
)