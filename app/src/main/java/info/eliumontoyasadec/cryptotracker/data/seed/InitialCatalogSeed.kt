package info.eliumontoyasadec.cryptotracker.data.seed

import info.eliumontoyasadec.cryptotracker.room.entities.CryptoEntity
import info.eliumontoyasadec.cryptotracker.room.entities.FiatEntity
import info.eliumontoyasadec.cryptotracker.room.entities.PortfolioEntity
import info.eliumontoyasadec.cryptotracker.room.entities.WalletEntity

object InitialCatalogSeed {

    val defaultPortfolio = PortfolioEntity(
        name = "Portafolio Principal",
        description = "Seed inicial",
        isDefault = true
    )

    fun walletsFor(portfolioId: Long): List<WalletEntity> = listOf(
        WalletEntity(portfolioId = portfolioId, name = "MetaMask", description = "Wallet principal", isMain = true),
        WalletEntity(portfolioId = portfolioId, name = "Bybit", description = "Exchange", isMain = false),
        WalletEntity(portfolioId = portfolioId, name = "Phantom", description = "Solana wallet", isMain = false),
    )

    val cryptos: List<CryptoEntity> = listOf(
        CryptoEntity(symbol = "BTC", name = "Bitcoin", coingeckoId = "bitcoin", isActive = true),
        CryptoEntity(symbol = "ETH", name = "Ethereum", coingeckoId = "ethereum", isActive = true),
        CryptoEntity(symbol = "SOL", name = "Solana", coingeckoId = "solana", isActive = true),
        CryptoEntity(symbol = "USDT", name = "Tether", coingeckoId = "tether", isActive = true),
    )

    val fiat: List<FiatEntity> = listOf(
        FiatEntity(code = "USD", name = "US Dollar", symbol = "$"),
        FiatEntity(code = "MXN", name = "Peso Mexicano", symbol = "$"),
        FiatEntity(code = "EUR", name = "Euro", symbol = "€"),
    )
}