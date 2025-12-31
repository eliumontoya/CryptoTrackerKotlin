package info.eliumontoyasadec.cryptotracker.ui.shell

import androidx.compose.runtime.staticCompositionLocalOf
import info.eliumontoyasadec.cryptotracker.data.seed.CatalogSeeder
import info.eliumontoyasadec.cryptotracker.domain.queries.PortfolioQueries


data class AppDeps(
    val portfolioQueries: PortfolioQueries,
    val catalogSeeder: CatalogSeeder

)

val LocalAppDeps = staticCompositionLocalOf<AppDeps> {
    error("AppDeps not provided. Provide it from MainActivity using CompositionLocalProvider.")
}