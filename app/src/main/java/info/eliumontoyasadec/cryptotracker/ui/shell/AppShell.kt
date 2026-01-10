package info.eliumontoyasadec.cryptotracker.ui.shell

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.NavigationDrawerItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberDrawerState
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import info.eliumontoyasadec.cryptotracker.ui.admin.cryptos.AdminCryptosScreen
import info.eliumontoyasadec.cryptotracker.ui.admin.delete.DeleteDataScreen
import info.eliumontoyasadec.cryptotracker.ui.admin.fiat.AdminFiatScreen
import info.eliumontoyasadec.cryptotracker.ui.admin.load.LoadInitialCatalogsScreen
import info.eliumontoyasadec.cryptotracker.ui.admin.portfolios.AdminPortfoliosScreen
import info.eliumontoyasadec.cryptotracker.ui.admin.setup.SetupInitialScreen
import info.eliumontoyasadec.cryptotracker.ui.admin.wallets.AdminWalletsScreen
import info.eliumontoyasadec.cryptotracker.ui.factories.PortfolioViewModelFactory
import info.eliumontoyasadec.cryptotracker.ui.portfolio.PortfolioScreen
import info.eliumontoyasadec.cryptotracker.ui.portfolio.PortfolioViewModel
import info.eliumontoyasadec.cryptotracker.ui.screens.CryptoDetailScreen
import info.eliumontoyasadec.cryptotracker.ui.screens.CryptoDetailViewModel
import info.eliumontoyasadec.cryptotracker.ui.screens.PortfolioByCryptosScreen
import info.eliumontoyasadec.cryptotracker.ui.screens.WalletBreakdownScreen
import info.eliumontoyasadec.cryptotracker.ui.screens.WalletBreakdownViewModel
import info.eliumontoyasadec.cryptotracker.ui.screens.WalletDetailScreen
import info.eliumontoyasadec.cryptotracker.ui.screens.WalletDetailViewModel
import info.eliumontoyasadec.cryptotracker.ui.screens.movements.CryptoFilter
import info.eliumontoyasadec.cryptotracker.ui.screens.movements.MovementDraft
import info.eliumontoyasadec.cryptotracker.ui.screens.movements.MovementFormMode
import info.eliumontoyasadec.cryptotracker.ui.screens.movements.MovementFormModelView
import info.eliumontoyasadec.cryptotracker.ui.screens.movements.MovementFormSheetContent
import info.eliumontoyasadec.cryptotracker.ui.screens.movements.MovementMode
import info.eliumontoyasadec.cryptotracker.ui.screens.movements.MovementsScreen
import info.eliumontoyasadec.cryptotracker.ui.screens.movements.MovementsViewModel
import info.eliumontoyasadec.cryptotracker.ui.screens.movements.SwapDraft
import info.eliumontoyasadec.cryptotracker.ui.screens.movements.SwapFormSheetContent
import info.eliumontoyasadec.cryptotracker.ui.screens.movements.WalletFilter
import kotlinx.coroutines.launch

// UI-only filter state (visual feedback only; not wired to ViewModels yet)
data class FilterUiState(
    val wallet: String = "Todas", val crypto: String = "Todas"
) {
    val hasActive: Boolean get() = wallet != "Todas" || crypto != "Todas"
    val label: String get() = "Filtros: $wallet · $crypto"

    fun cleared(): FilterUiState = FilterUiState()
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppShell() {
    val navController = rememberNavController()
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()

    var showSearchDialog by remember { mutableStateOf(false) }
    var showFilterDialog by remember { mutableStateOf(false) }
    var showAddMovementDialog by remember { mutableStateOf(false) }
    var addMovementDraft by remember { mutableStateOf(MovementDraft()) }
    var showRefreshDialog by remember { mutableStateOf(false) }

    var showAddMenu by remember { mutableStateOf(false) }
    var showAddSwapSheet by remember { mutableStateOf(false) }
    var addSwapDraft by remember { mutableStateOf(SwapDraft()) }

    // UI-only (fake) active filters for visual feedback in TopAppBar
    var filters by remember { mutableStateOf(FilterUiState()) }

    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route

    // NOTE: `destination.route` is the pattern (e.g., "crypto_detail/{symbol}") so we must read args.
    val isCryptoDetail = currentRoute == AppDestination.CryptoDetail.route
    val cryptoSymbol = backStackEntry?.arguments?.getString("symbol")?.uppercase()

    val isWalletDetail = currentRoute == AppDestination.WalletDetail.route
    val walletName = backStackEntry?.arguments?.getString("wallet")

    // ✅ Detail screens = show Back arrow
    // (No tocamos el NavHost; esto solo cambia el ícono del TopAppBar)
    val detailRoutes = setOf(
        AppDestination.CryptoDetail.route,
        AppDestination.WalletDetail.route,

        // Admin sub-screens (opened from Setup Inicial)
        AppDestination.AdminDeleteData.route,
        AppDestination.AdminSetupCatalogs.route,
        AppDestination.AdminCryptos.route,
        AppDestination.AdminWallets.route,
        AppDestination.AdminFiat.route,
        AppDestination.AdminPortfolio.route
    )

    val isDetailScreen = currentRoute in detailRoutes

    // ⚠️ OJO: tus acciones actuales están diseñadas para que SOLO CryptoDetail/WalletDetail
    // muestren el menú de "Agregar" y "Refrescar".
    // Por eso, isListScreen se mantiene como estaba (basado solo en crypto/wallet detail).
    val isListScreen = !(isCryptoDetail || isWalletDetail)

    val snackbarHostState = remember { SnackbarHostState() }
    ModalNavigationDrawer(drawerState = drawerState, drawerContent = {
        AppDrawer(currentRoute = currentRoute, onNavigate = { route ->
            scope.launch {
                navController.navigate(route) {
                    launchSingleTop = true
                }
                drawerState.close()
            }
        })
    }) {
        Scaffold(topBar = {
            TopAppBar(title = {
                Text(
                    text = when {
                        isCryptoDetail -> cryptoSymbol ?: "Crypto"
                        isWalletDetail -> walletName ?: "Cartera"
                        else -> routeToTitle(currentRoute)
                    }
                )
            }, navigationIcon = {
                if (isDetailScreen) {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                } else {
                    IconButton(onClick = { scope.launch { drawerState.open() } }) {
                        Icon(
                            imageVector = Icons.Default.Menu, contentDescription = "Menú"
                        )
                    }
                }
            }, actions = {
                if (isListScreen) {
                    if (filters.hasActive) {
                        Text(
                            text = filters.label,
                            style = MaterialTheme.typography.labelLarge,
                            modifier = Modifier.padding(end = 8.dp)
                        )
                        IconButton(onClick = { filters = filters.cleared() }) {
                            Icon(Icons.Filled.Close, contentDescription = "Limpiar filtros")
                        }
                    }

                    IconButton(onClick = { showSearchDialog = true }) {
                        Icon(Icons.Filled.Search, contentDescription = "Buscar")
                    }
                    IconButton(onClick = { showFilterDialog = true }) {
                        Icon(Icons.Filled.FilterList, contentDescription = "Filtros")
                    }
                } else {
                    Box {
                        IconButton(onClick = { showAddMenu = true }) {
                            Icon(Icons.Filled.Add, contentDescription = "Agregar")
                        }

                        DropdownMenu(
                            expanded = showAddMenu,
                            onDismissRequest = { showAddMenu = false }) {
                            DropdownMenuItem(text = { Text("Movimiento") }, onClick = {
                                val prefilled = when {
                                    isCryptoDetail -> {
                                        val crypto = cryptoSymbol?.let { symbolToCryptoFilter(it) }
                                            ?: CryptoFilter.BTC
                                        MovementDraft(crypto = crypto)
                                    }

                                    isWalletDetail -> {
                                        val wallet = walletName?.let { nameToWalletFilter(it) }
                                            ?: WalletFilter.METAMASK
                                        MovementDraft(wallet = wallet)
                                    }

                                    else -> MovementDraft()
                                }
                                addMovementDraft = prefilled
                                showAddMovementDialog = true
                                showAddMenu = false
                            })

                            DropdownMenuItem(text = { Text("Swap") }, onClick = {
                                val prefilledSwap = when {
                                    isCryptoDetail -> {
                                        val crypto = cryptoSymbol?.let { symbolToCryptoFilter(it) }
                                            ?: CryptoFilter.BTC
                                        SwapDraft(fromCrypto = crypto)
                                    }

                                    isWalletDetail -> {
                                        val wallet = walletName?.let { nameToWalletFilter(it) }
                                            ?: WalletFilter.METAMASK
                                        SwapDraft(wallet = wallet)
                                    }

                                    else -> SwapDraft()
                                }
                                addSwapDraft = prefilledSwap
                                showAddSwapSheet = true
                                showAddMenu = false
                            })
                        }
                    }
                    IconButton(onClick = { showRefreshDialog = true }) {
                        Icon(Icons.Filled.Refresh, contentDescription = "Refrescar")
                    }
                }
            })
        }, snackbarHost = { SnackbarHost(hostState = snackbarHostState) }) { padding ->
            NavHost(
                navController = navController,
                startDestination = AppDestination.Portfolio.route,
                modifier = Modifier.padding(padding)
            ) {
                composable(AppDestination.Portfolio.route) {
                    val queries =
                        LocalAppDeps.current.portfolioQueries // te explico abajo cómo crear esto
                    val vm: PortfolioViewModel = viewModel(
                        factory = PortfolioViewModelFactory(
                            portfolioId = 1L, // temporal: default portfolio
                            queries = queries
                        )
                    )

                    val state = vm.state.collectAsState().value

                    PortfolioScreen(state = state, onRowClick = { symbol ->
                        navController.navigate("crypto_detail/$symbol") {
                            launchSingleTop = true
                        }
                    })
                }
                composable(AppDestination.PortfolioByCrypto.route) {
                    PortfolioByCryptosScreen()
                }
                composable(AppDestination.WalletBreakdown.route) {
                    val vm: WalletBreakdownViewModel = viewModel()
                    val state = vm.state.collectAsState().value

                    WalletBreakdownScreen(
                        state = state,
                        onToggleShowEmpty = vm::toggleShowEmpty,
                        onChangeSort = vm::changeSort,
                        onWalletClick = { wallet ->
                            navController.navigate("wallet_detail/$wallet") {
                                launchSingleTop = true
                            }
                        },
                        onAddMovement = vm::startAddMovement,
                        onDismissForm = vm::dismissForm,
                        onMovementDraftChange = vm::changeMovementDraft,
                        onMovementSave = {
                            vm.saveMovement()
                            scope.launch { snackbarHostState.showSnackbar("Movimiento guardado") }
                        })
                }

                composable(AppDestination.InMovements.route) {
                    val deps = LocalAppDeps.current
                    val vm: MovementsViewModel = viewModel(
                        key = "movements-in",
                        factory = deps.movementsViewModelFactory(MovementMode.IN)
                    )

                    val state = vm.state.collectAsState().value

                    MovementsScreen(
                        title = "Movimientos de Entrada",
                        state = state,
                        onSelectWallet = vm::selectWallet,
                        onSelectCrypto = vm::selectCrypto,
                        onCreate = vm::startCreate,
                        onEdit = vm::startEdit,
                        onRequestDelete = vm::requestDelete,
                        onCancelDelete = vm::cancelDelete,
                        onConfirmDelete = {
                            vm.confirmDelete(it)
                            scope.launch { snackbarHostState.showSnackbar("Movimiento eliminado") }
                        },
                        onDismissForms = vm::dismissForms,
                        onMovementDraftChange = vm::changeMovementDraft,
                        onMovementSave = {
                            vm.saveMovement()
                            scope.launch { snackbarHostState.showSnackbar("Movimiento guardado") }
                        },
                        onSwapDraftChange = vm::changeSwapDraft,
                        onSwapSave = {
                            vm.saveSwap()
                            scope.launch { snackbarHostState.showSnackbar("Swap guardado") }
                        })
                }
                composable(AppDestination.OutMovements.route) {
                    val vm: MovementsViewModel = viewModel(
                        key = "movements-out",
                        factory = LocalAppDeps.current.movementsViewModelFactory(MovementMode.OUT)
                    )

                    val state = vm.state.collectAsState().value

                    MovementsScreen(
                        title = "Movimientos de Salida",
                        state = state,
                        onSelectWallet = vm::selectWallet,
                        onSelectCrypto = vm::selectCrypto,
                        onCreate = vm::startCreate,
                        onEdit = vm::startEdit,
                        onRequestDelete = vm::requestDelete,
                        onCancelDelete = vm::cancelDelete,
                        onConfirmDelete = {
                            vm.confirmDelete(it)
                            scope.launch { snackbarHostState.showSnackbar("Movimiento eliminado") }
                        },
                        onDismissForms = vm::dismissForms,
                        onMovementDraftChange = vm::changeMovementDraft,
                        onMovementSave = {
                            vm.saveMovement()
                            scope.launch { snackbarHostState.showSnackbar("Movimiento guardado") }
                        },
                        onSwapDraftChange = vm::changeSwapDraft,
                        onSwapSave = {
                            vm.saveSwap()
                            scope.launch { snackbarHostState.showSnackbar("Swap guardado") }
                        })
                }
                composable(AppDestination.BetweenWallets.route) {
                     val vm: MovementsViewModel = viewModel(
                        key = "movements-between",
                        factory = LocalAppDeps.current.movementsViewModelFactory(MovementMode.BETWEEN)
                    )
                    val state = vm.state.collectAsState().value

                    MovementsScreen(
                        title = "Movimientos Entre Carteras",
                        state = state,
                        onSelectWallet = vm::selectWallet,
                        onSelectCrypto = vm::selectCrypto,
                        onCreate = vm::startCreate,
                        onEdit = vm::startEdit,
                        onRequestDelete = vm::requestDelete,
                        onCancelDelete = vm::cancelDelete,
                        onConfirmDelete = {
                            vm.confirmDelete(it)
                            scope.launch { snackbarHostState.showSnackbar("Movimiento eliminado") }
                        },
                        onDismissForms = vm::dismissForms,
                        onMovementDraftChange = vm::changeMovementDraft,
                        onMovementSave = {
                            vm.saveMovement()
                            scope.launch { snackbarHostState.showSnackbar("Movimiento guardado") }
                        },
                        onSwapDraftChange = vm::changeSwapDraft,
                        onSwapSave = {
                            vm.saveSwap()
                            scope.launch { snackbarHostState.showSnackbar("Swap guardado") }
                        })
                }
                composable(AppDestination.Swaps.route) {

                     val vm: MovementsViewModel = viewModel(
                        key = "movements-swap",
                        factory = LocalAppDeps.current.movementsViewModelFactory(MovementMode.SWAP)
                    )

                    val state = vm.state.collectAsState().value

                    MovementsScreen(
                        title = "Movimientos de Swaps",
                        state = state,
                        onSelectWallet = vm::selectWallet,
                        onSelectCrypto = vm::selectCrypto,
                        onCreate = vm::startCreate,
                        onEdit = vm::startEdit,
                        onRequestDelete = vm::requestDelete,
                        onCancelDelete = vm::cancelDelete,
                        onConfirmDelete = {
                            vm.confirmDelete(it)
                            scope.launch { snackbarHostState.showSnackbar("Movimiento eliminado") }
                        },
                        onDismissForms = vm::dismissForms,
                        onMovementDraftChange = vm::changeMovementDraft,
                        onMovementSave = {
                            vm.saveMovement()
                            scope.launch { snackbarHostState.showSnackbar("Movimiento guardado") }
                        },
                        onSwapDraftChange = vm::changeSwapDraft,
                        onSwapSave = {
                            vm.saveSwap()
                            scope.launch { snackbarHostState.showSnackbar("Swap guardado") }
                        })
                }

                composable(AppDestination.Admin.route) {
                    // Admin = Setup Inicial
                    SetupInitialScreen(
                        onDeleteAllData = { navController.navigate(AppDestination.AdminDeleteData.route) },
                        onLoadInitialCatalogs = { navController.navigate(AppDestination.AdminSetupCatalogs.route) },
                        onLoadInitialMovements = { scope.launch { snackbarHostState.showSnackbar("Carga movimientos (pendiente)") } },
                        onBackupExport = { scope.launch { snackbarHostState.showSnackbar("Generar backup (pendiente)") } },
                        onBackupImport = { scope.launch { snackbarHostState.showSnackbar("Cargar backup (pendiente)") } })
                }
                composable(AppDestination.AdminDeleteData.route) {
                    DeleteDataScreen(onClose = { navController.popBackStack() })
                }
                composable(AppDestination.AdminCryptos.route) {
                    AdminCryptosScreen(
                    )
                }

                composable(AppDestination.AdminWallets.route) {
                    AdminWalletsScreen(
                    )
                }

                composable(AppDestination.AdminFiat.route) {
                    AdminFiatScreen(
                    )
                }

                composable(AppDestination.AdminPortfolio.route) {
                    AdminPortfoliosScreen(
                    )
                }

                composable(
                    route = AppDestination.CryptoDetail.route,
                    arguments = listOf(navArgument("symbol") { type = NavType.StringType })
                ) { entry ->
                    val symbol = entry.arguments?.getString("symbol").orEmpty()
                    val vm: CryptoDetailViewModel = viewModel(
                        factory = CryptoDetailViewModel.Factory(symbol)
                    )
                    val state = vm.state.collectAsState().value

                    CryptoDetailScreen(state = state)
                }
                composable(
                    route = AppDestination.WalletDetail.route,
                    arguments = listOf(navArgument("wallet") { type = NavType.StringType })
                ) { entry ->
                    val wallet = entry.arguments?.getString("wallet").orEmpty()
                    val vm: WalletDetailViewModel = viewModel(
                        factory = WalletDetailViewModel.Factory(wallet)
                    )
                    val state = vm.state.collectAsState().value

                    WalletDetailScreen(state = state)
                }
                composable(AppDestination.AdminSetupCatalogs.route) {
                    LoadInitialCatalogsScreen(onClose = { navController.popBackStack() })
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
            text = { Text("Búsqueda (fake). Próximo paso: campo de texto + filtrado en UI.") })
    }

    if (showFilterDialog) {
        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        var selectedWallet by remember { mutableStateOf(filters.wallet) }
        var selectedCrypto by remember { mutableStateOf(filters.crypto) }

        ModalBottomSheet(
            onDismissRequest = { showFilterDialog = false }, sheetState = sheetState
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text("Filtros", style = MaterialTheme.typography.titleLarge)
                Text(
                    "(fake) Próximo paso: conectar esto al estado del ViewModel.",
                    style = MaterialTheme.typography.bodySmall
                )

                Text("Cartera", style = MaterialTheme.typography.labelLarge)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("Todas", "Metamask", "ByBit", "Phantom").forEach { label ->
                        FilterChip(
                            selected = selectedWallet == label,
                            onClick = { selectedWallet = label },
                            label = { Text(label) })
                    }
                }

                Text("Crypto", style = MaterialTheme.typography.labelLarge)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("Todas", "BTC", "ETH", "SOL", "ALGO", "AIXBT").forEach { label ->
                        FilterChip(
                            selected = selectedCrypto == label,
                            onClick = { selectedCrypto = label },
                            label = { Text(label) })
                    }
                }

                Text("Rango de fechas", style = MaterialTheme.typography.labelLarge)
                Text(
                    "Desde: (pendiente)   Hasta: (pendiente)",
                    style = MaterialTheme.typography.bodyMedium
                )

                Spacer(Modifier.height(4.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = { showFilterDialog = false }) { Text("Cancelar") }
                    TextButton(onClick = {
                        filters = FilterUiState(wallet = selectedWallet, crypto = selectedCrypto)
                        showFilterDialog = false
                    }) { Text("Aplicar") }
                }

                Spacer(Modifier.height(12.dp))
            }
        }
    }

    if (showAddMovementDialog) {
        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        val formMv = remember(showAddMovementDialog) {
            MovementFormModelView(
                initialMode = MovementFormMode.CREATE,
                initialDraft = addMovementDraft,
                onCancelExternal = { showAddMovementDialog = false },
                onDraftChangeExternal = { addMovementDraft = it },
                onSaveExternal = {
                    // UI-only: close the sheet. (Later: call use case / repo)
                    showAddMovementDialog = false
                    scope.launch { snackbarHostState.showSnackbar("Movimiento guardado") }
                }
            )
        }

        ModalBottomSheet(
            onDismissRequest = { showAddMovementDialog = false },
            sheetState = sheetState
        ) {
            MovementFormSheetContent(
                state = formMv.state,
                mv = formMv
            )
        }

    }

    if (showAddSwapSheet) {
        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ModalBottomSheet(
            onDismissRequest = { showAddSwapSheet = false }, sheetState = sheetState
        ) {
            SwapFormSheetContent(
                draft = addSwapDraft,
                onChange = { addSwapDraft = it },
                onCancel = { showAddSwapSheet = false },
                onSave = {
                    // UI-only: close the sheet. (Later: call use case / repo)
                    showAddSwapSheet = false
                    scope.launch { snackbarHostState.showSnackbar("Swap guardado") }
                })
        }
    }

    if (showRefreshDialog) {
        AlertDialog(
            onDismissRequest = { showRefreshDialog = false },
            confirmButton = {
                TextButton(onClick = { showRefreshDialog = false }) { Text("Entendido") }
            },
            title = { Text("Refrescar") },
            text = { Text("Refrescar (fake). Con wiring, esto reconsultará queries o invalidará caché.") })
    }
}

@Composable
private fun AppDrawer(
    currentRoute: String?, onNavigate: (String) -> Unit
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
    onNavigate: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    NavigationDrawerItem(
        label = { Text(dest.label) },
        selected = currentRoute == dest.route,
        onClick = { onNavigate(dest.route) },
        icon = { Icon(dest.icon, contentDescription = dest.label) },
        modifier = modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
    )
}

private fun routeToTitle(route: String?): String = when (route) {
    AppDestination.Portfolio.route -> "Portafolio"
    AppDestination.PortfolioByCrypto.route -> "Portafolio por Cryptos"
    AppDestination.WalletBreakdown.route -> "Desglose por Carteras"
    AppDestination.InMovements.route -> "Entrada"
    AppDestination.OutMovements.route -> "Salida"
    AppDestination.BetweenWallets.route -> "Entre Carteras"
    AppDestination.Swaps.route -> "Swaps"
    AppDestination.Admin.route -> "Setup Inicial"
    AppDestination.AdminDeleteData.route -> "Eliminar datos"
    AppDestination.AdminSetupCatalogs.route -> "Cargar catálogos"
    AppDestination.AdminCryptos.route -> "Cryptos"
    AppDestination.AdminWallets.route -> "Carteras"
    AppDestination.AdminFiat.route -> "FIAT"
    AppDestination.AdminPortfolio.route -> "Portafolio"
    else -> "CryptoTracker"
}

private fun symbolToCryptoFilter(symbol: String): CryptoFilter {
    return try {
        CryptoFilter.valueOf(symbol.trim().uppercase())
    } catch (_: Throwable) {
        CryptoFilter.BTC
    }
}

private fun nameToWalletFilter(name: String): WalletFilter {
    val n = name.trim().lowercase()
    return when {
        n.contains("meta") -> WalletFilter.METAMASK
        n.contains("bybit") -> WalletFilter.BYBIT
        n.contains("phantom") -> WalletFilter.PHANTOM
        else -> WalletFilter.METAMASK
    }
}