package info.eliumontoyasadec.cryptotracker.domain.model
data class Wallet (
    val walletId: Long,
    val portfolioId: Long,     // <-- NUEVO
    val name: String,
    val description: String?,
    val isMain: Boolean
)