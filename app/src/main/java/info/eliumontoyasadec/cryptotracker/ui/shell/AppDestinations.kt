// AppDestinations.kt
package info.eliumontoyasadec.cryptotracker.ui.shell

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.ui.graphics.vector.ImageVector

sealed class AppDestination(
    val route: String,
    val label: String,
    val icon: ImageVector
) {
    data object Portfolio : AppDestination("portfolio", "Portafolio", Icons.Filled.PieChart)
    data object PortfolioByCrypto : AppDestination("portfolio_by_crypto", "Por Crypto", Icons.Filled.TableChart)
    data object WalletBreakdown : AppDestination("wallet_breakdown", "Por Cartera", Icons.Filled.AccountBalanceWallet)

    data object InMovements : AppDestination("mov_in", "Entrada", Icons.Filled.ArrowDownward)
    data object OutMovements : AppDestination("mov_out", "Salida", Icons.Filled.ArrowUpward)
    data object BetweenWallets : AppDestination("mov_between", "Entre Carteras", Icons.Filled.SwapHoriz)
    data object Swaps : AppDestination("mov_swaps", "Swaps", Icons.Filled.SyncAlt)

    // PADRE: seguirá existiendo en el menú como "Administración"
    // y SU SCREEN será Setup Inicial.
    data object Admin : AppDestination("admin", "Config Inicial", Icons.Filled.Settings)

    // HIJOS bajo Administración (en el Drawer)
    data object AdminCryptos : AppDestination("admin/cryptos", "Cryptos", Icons.Filled.CurrencyExchange)
    data object AdminWallets : AppDestination("admin/wallets", "Carteras", Icons.Filled.AccountBalanceWallet)
    data object AdminFiat : AppDestination("admin/fiat", "FIAT", Icons.Filled.AttachMoney)
    data object AdminPortfolio : AppDestination("admin/portfolio", "Portafolio", Icons.Filled.PieChart)

    data object CryptoDetail : AppDestination("crypto_detail/{symbol}", "Detalle Crypto", Icons.Filled.Info)
    data object WalletDetail : AppDestination("wallet_detail/{wallet}", "Detalle Cartera", Icons.Filled.Info)
}

val mainMenu: List<AppDestination> = listOf(
    AppDestination.Portfolio,
    AppDestination.PortfolioByCrypto,
    AppDestination.WalletBreakdown
)

val movementsMenu: List<AppDestination> = listOf(
    AppDestination.InMovements,
    AppDestination.OutMovements,
    AppDestination.BetweenWallets,
    AppDestination.Swaps
)

// El padre sigue siendo solo "Administración"
val adminMenu: List<AppDestination> = listOf(
    AppDestination.Admin,
    AppDestination.AdminCryptos,
    AppDestination.AdminWallets,
    AppDestination.AdminFiat,
    AppDestination.AdminPortfolio
)

