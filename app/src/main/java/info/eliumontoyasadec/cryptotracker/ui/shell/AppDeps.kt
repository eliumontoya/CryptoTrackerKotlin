package info.eliumontoyasadec.cryptotracker.ui.shell

import androidx.compose.runtime.staticCompositionLocalOf
import info.eliumontoyasadec.cryptotracker.data.seed.CatalogSeeder
import info.eliumontoyasadec.cryptotracker.data.seed.DatabaseWiper
import info.eliumontoyasadec.cryptotracker.domain.queries.PortfolioQueries
import info.eliumontoyasadec.cryptotracker.domain.repositories.CryptoRepository
import info.eliumontoyasadec.cryptotracker.domain.repositories.FiatRepository
import info.eliumontoyasadec.cryptotracker.domain.repositories.PortfolioRepository
import info.eliumontoyasadec.cryptotracker.domain.repositories.WalletRepository


data class AppDeps(
     val portfolioQueries: PortfolioQueries,
    val catalogSeeder: CatalogSeeder,
    val databaseWiper: DatabaseWiper,
    val portfolioRepository: PortfolioRepository,
     val walletRepository: WalletRepository,
     val cryptoRepository: CryptoRepository,
    val fiatRepository: FiatRepository



)

val LocalAppDeps = staticCompositionLocalOf<AppDeps> {
    error("AppDeps not provided. Provide it from MainActivity using CompositionLocalProvider.")
}