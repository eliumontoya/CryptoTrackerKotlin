package info.eliumontoyasadec.cryptotracker.domain.model

data class Crypto(
    val symbol: String,        // btc, eth
    val name: String,          // Bitcoin
    val coingeckoId: String?,  // bitcoin
    val isActive: Boolean = true
)