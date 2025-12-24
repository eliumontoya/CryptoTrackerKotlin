package info.eliumontoyasadec.cryptotracker.room.queries

data class PortfolioWalletTotalRow(
    val portfolioId: Long,
    val walletId: Long,
    val walletName: String,
    val totalDistinctCryptos: Int,
    val lastUpdatedAt: Long?
)

data class WalletHoldingRow(
    val walletId: Long,
    val walletName: String,
    val cryptoSymbol: String,
    val cryptoName: String?,
    val quantity: Double,
    val updatedAt: Long
)

data class PortfolioTotalRow(
    val portfolioId: Long,
    val portfolioName: String,
    val totalWallets: Int,
    val totalDistinctCryptos: Int,
    val lastUpdatedAt: Long?
)