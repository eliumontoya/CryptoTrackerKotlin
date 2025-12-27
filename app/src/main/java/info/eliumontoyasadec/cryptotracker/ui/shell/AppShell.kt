package info.eliumontoyasadec.cryptotracker.ui.shell

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import info.eliumontoyasadec.cryptotracker.ui.portfolio.PortfolioFakeData
import info.eliumontoyasadec.cryptotracker.ui.portfolio.PortfolioScreen
import info.eliumontoyasadec.cryptotracker.ui.screens.MovementMode
import info.eliumontoyasadec.cryptotracker.ui.screens.MovementsScreen
import info.eliumontoyasadec.cryptotracker.ui.screens.PlaceholderScreen
import info.eliumontoyasadec.cryptotracker.ui.screens.PortfolioByCryptosScreen
import info.eliumontoyasadec.cryptotracker.ui.screens.WalletBreakdownScreen
import kotlinx.coroutines.launch
import info.eliumontoyasadec.cryptotracker.ui.screens.CryptoDetailScreen
import androidx.navigation.NavType
import androidx.navigation.navArgument
import info.eliumontoyasadec.cryptotracker.ui.screens.WalletBreakdownRow
import info.eliumontoyasadec.cryptotracker.ui.screens.WalletDetailScreen

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppShell() {
    val navController = rememberNavController()
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()

    var showSearchDialog by remember { mutableStateOf(false) }
    var showFilterDialog by remember { mutableStateOf(false) }
    var showAddMovementDialog by remember { mutableStateOf(false) }
    var showRefreshDialog by remember { mutableStateOf(false) }

    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route

    // NOTE: `destination.route` is the pattern (e.g., "crypto_detail/{symbol}") so we must read args.
    val isCryptoDetail = currentRoute == AppDestination.CryptoDetail.route
    val cryptoSymbol = backStackEntry?.arguments?.getString("symbol")?.uppercase()

    val isWalletDetail = currentRoute == AppDestination.WalletDetail.route
    val walletName = backStackEntry?.arguments?.getString("wallet")

    val isDetailScreen = isCryptoDetail || isWalletDetail
    val isListScreen = !isDetailScreen

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            AppDrawer(
                currentRoute = currentRoute,
                onNavigate = { route ->
                    scope.launch {
                        navController.navigate(route) {
                            launchSingleTop = true
                        }
                        drawerState.close()
                    }
                }
            )
        }
    ) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = {
                        Text(
                            text = when {
                                isCryptoDetail -> cryptoSymbol ?: "Crypto"
                                isWalletDetail -> walletName ?: "Cartera"
                                else -> routeToTitle(currentRoute)
                            }
                        )
                    },
                    navigationIcon = {
                        if (isDetailScreen) {
                            IconButton(onClick = { navController.popBackStack() }) {
                                Icon(
                                    imageVector = Icons.Default.ArrowBack,
                                    contentDescription = "Back"
                                )
                            }
                        } else {
                            IconButton(onClick = { scope.launch { drawerState.open() } }) {
                                Icon(
                                    imageVector = Icons.Default.Menu,
                                    contentDescription = "Menú"
                                )
                            }
                        }
                    },
                    actions = {
                        if (isListScreen) {
                            IconButton(onClick = { showSearchDialog = true }) {
                                Icon(Icons.Filled.Search, contentDescription = "Buscar")
                            }
                            IconButton(onClick = { showFilterDialog = true }) {
                                Icon(Icons.Filled.FilterList, contentDescription = "Filtros")
                            }
                        } else {
                            IconButton(onClick = { showAddMovementDialog = true }) {
                                Icon(Icons.Filled.Add, contentDescription = "Agregar movimiento")
                            }
                            IconButton(onClick = { showRefreshDialog = true }) {
                                Icon(Icons.Filled.Refresh, contentDescription = "Refrescar")
                            }
                        }
                    }
                )
            }
        ) { padding ->
            NavHost(
                navController = navController,
                startDestination = AppDestination.Portfolio.route,
                modifier = Modifier.padding(padding)
            ) {
                composable(AppDestination.Portfolio.route) {
                    PortfolioScreen(
                        state = PortfolioFakeData.sample,
                        onRowClick = { symbol ->
                            navController.navigate("crypto_detail/$symbol") { launchSingleTop = true }
                        }
                    )
                }
                composable(AppDestination.PortfolioByCrypto.route) {
                    PortfolioByCryptosScreen()
                }
                composable(AppDestination.WalletBreakdown.route) {
                    val walletRowsFake = listOf(
                        WalletBreakdownRow("Metamask", 10400.0, 650.0),
                        WalletBreakdownRow("ByBit", 2100.0, 190.0),
                        WalletBreakdownRow("Phantom", 0.0, 0.0),
                    )

                    WalletBreakdownScreen(
                        rows = walletRowsFake,
                        onWalletClick = { wallet ->
                            navController.navigate("wallet_detail/$wallet") { launchSingleTop = true }
                        }
                    )                }

                composable(AppDestination.InMovements.route) {
                    MovementsScreen("Movimientos de Entrada", MovementMode.IN)
                }
                composable(AppDestination.OutMovements.route) {
                    MovementsScreen("Movimientos de Salida", MovementMode.OUT)
                }
                composable(AppDestination.BetweenWallets.route) {
                    MovementsScreen("Movimientos Entre Carteras", MovementMode.BETWEEN)
                }
                composable(AppDestination.Swaps.route) {
                    MovementsScreen("Movimientos de Swaps", MovementMode.SWAP)
                }

                composable(AppDestination.Admin.route) {
                    PlaceholderScreen(
                        title = "Administración",
                        subtitle = "Pantalla de configuración (fake)."
                    )
                }

                composable(
                    route = AppDestination.CryptoDetail.route,
                    arguments = listOf(navArgument("symbol") { type = NavType.StringType })
                ) { backStackEntry ->
                    val symbol = backStackEntry.arguments?.getString("symbol").orEmpty()
                    CryptoDetailScreen(symbol = symbol)
                }
                composable(
                    route = AppDestination.WalletDetail.route,
                    arguments = listOf(navArgument("wallet") { type = NavType.StringType })
                ) { entry ->
                    val wallet = entry.arguments?.getString("wallet").orEmpty()
                    WalletDetailScreen(walletName = wallet)
                }
            }
        }
    }

    if (showSearchDialog) {
        AlertDialog(
            onDismissRequest = { showSearchDialog = false },
            confirmButton = {
                TextButton(onClick = { showSearchDialog = false }) { Text("Cerrar") }
            },
            title = { Text("Buscar") },
            text = { Text("Búsqueda (fake). Próximo paso: campo de texto + filtrado en UI.") }
        )
    }

    if (showFilterDialog) {
        AlertDialog(
            onDismissRequest = { showFilterDialog = false },
            confirmButton = {
                TextButton(onClick = { showFilterDialog = false }) { Text("Aplicar") }
            },
            dismissButton = {
                TextButton(onClick = { showFilterDialog = false }) { Text("Cancelar") }
            },
            title = { Text("Filtros") },
            text = { Text("Filtros (fake). Próximo paso: chips por Wallet/Crypto/Tipo/Fechas.") }
        )
    }

    if (showAddMovementDialog) {
        AlertDialog(
            onDismissRequest = { showAddMovementDialog = false },
            confirmButton = {
                TextButton(onClick = { showAddMovementDialog = false }) { Text("Guardar") }
            },
            dismissButton = {
                TextButton(onClick = { showAddMovementDialog = false }) { Text("Cancelar") }
            },
            title = { Text("Agregar movimiento") },
            text = { Text("Formulario (fake). Próximo paso: ModalBottomSheet con tipo/cantidad/precio/wallet.") }
        )
    }

    if (showRefreshDialog) {
        AlertDialog(
            onDismissRequest = { showRefreshDialog = false },
            confirmButton = {
                TextButton(onClick = { showRefreshDialog = false }) { Text("Entendido") }
            },
            title = { Text("Refrescar") },
            text = { Text("Refrescar (fake). Con wiring, esto reconsultará queries o invalidará caché.") }
        )
    }
}

@Composable
private fun AppDrawer(
    currentRoute: String?,
    onNavigate: (String) -> Unit
) {
    ModalDrawerSheet {
        Text(
            text = "CryptoTracker",
            style = MaterialTheme.typography.titleLarge,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
        )

        DrawerSectionTitle("Portafolio")
        mainMenu.forEach { dest -> DrawerItem(dest, currentRoute, onNavigate) }

        DrawerSectionTitle("Movimientos")
        movementsMenu.forEach { dest -> DrawerItem(dest, currentRoute, onNavigate) }

        DrawerSectionTitle("Administración")
        adminMenu.forEach { dest -> DrawerItem(dest, currentRoute, onNavigate) }
    }
}

@Composable
private fun DrawerSectionTitle(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelLarge,
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
    )
}

@Composable
private fun DrawerItem(
    dest: AppDestination,
    currentRoute: String?,
    onNavigate: (String) -> Unit
) {
    NavigationDrawerItem(
        label = { Text(dest.label) },
        selected = currentRoute == dest.route,
        onClick = { onNavigate(dest.route) },
        icon = { Icon(dest.icon, contentDescription = dest.label) },
        modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
    )
}

@Composable
private fun currentRoute(navController: NavHostController): String? {
    val backStackEntry by navController.currentBackStackEntryAsState()
    return backStackEntry?.destination?.route
}

private fun routeToTitle(route: String?): String = when (route) {
    AppDestination.Portfolio.route -> "Portafolio"
    AppDestination.PortfolioByCrypto.route -> "Portafolio por Cryptos"
    AppDestination.WalletBreakdown.route -> "Desglose por Carteras"
    AppDestination.InMovements.route -> "Entrada"
    AppDestination.OutMovements.route -> "Salida"
    AppDestination.BetweenWallets.route -> "Entre Carteras"
    AppDestination.Swaps.route -> "Swaps"
    AppDestination.Admin.route -> "Administración"
    else -> "CryptoTracker"
}