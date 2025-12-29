package info.eliumontoyasadec.cryptotracker.ui.admin

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

enum class AdminSection(
    val label: String,
    val icon: @Composable () -> Unit
) {
    CRYPTOS("Cryptos", { Icon(Icons.Default.CurrencyExchange, contentDescription = null) }),
    WALLETS("Carteras", { Icon(Icons.Default.AccountBalanceWallet, contentDescription = null) }),
    FIAT("FIAT", { Icon(Icons.Default.AttachMoney, contentDescription = null) }),
    PORTFOLIO("Portafolio", { Icon(Icons.Default.PieChart, contentDescription = null) }),
    SETUP("Setup inicial", { Icon(Icons.Default.Build, contentDescription = null) }),
}

@Composable
fun AdminScreen(
    modifier: Modifier = Modifier,
    initialSection: AdminSection = AdminSection.SETUP,
    onDeleteAllData: () -> Unit = {},
    onLoadInitialCatalogs: () -> Unit = {},
    onLoadInitialMovements: () -> Unit = {},
    onBackupExport: () -> Unit = {},
    onBackupImport: () -> Unit = {},
) {
    var selected by remember { mutableStateOf(initialSection) }

    Row(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        AdminSideMenu(
            selected = selected,
            onSelect = { selected = it }
        )

        // Contenido principal: mismo estilo que PortfolioScreen (Column + spacing)
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            AdminHeader(title = "Administración", subtitle = selected.label)

            when (selected) {
                AdminSection.SETUP -> SetupInitialScreen(
                    onDeleteAllData = onDeleteAllData,
                    onLoadInitialCatalogs = onLoadInitialCatalogs,
                    onLoadInitialMovements = onLoadInitialMovements,
                    onBackupExport = onBackupExport,
                    onBackupImport = onBackupImport
                )

                AdminSection.CRYPTOS -> AdminPlaceholderCard(
                    title = "Cryptos",
                    subtitle = "Catálogo de cryptos (pendiente de wiring)."
                )

                AdminSection.WALLETS -> AdminPlaceholderCard(
                    title = "Carteras",
                    subtitle = "Administración de carteras (pendiente de wiring)."
                )

                AdminSection.FIAT -> AdminPlaceholderCard(
                    title = "FIAT",
                    subtitle = "Catálogo de monedas fiat (pendiente de wiring)."
                )

                AdminSection.PORTFOLIO -> AdminPlaceholderCard(
                    title = "Portafolio",
                    subtitle = "Gestión de portafolios (pendiente de wiring)."
                )
            }
        }
    }
}

@Composable
private fun AdminHeader(title: String, subtitle: String) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(text = title, style = MaterialTheme.typography.headlineSmall)
        Text(text = subtitle, style = MaterialTheme.typography.bodySmall)
    }
}

@Composable
private fun AdminSideMenu(
    selected: AdminSection,
    onSelect: (AdminSection) -> Unit
) {
    // Esto emula tu screenshot (menú vertical lateral)
    NavigationRail {
        Spacer(Modifier.height(8.dp))
        AdminSection.entries.forEach { section ->
            NavigationRailItem(
                selected = selected == section,
                onClick = { onSelect(section) },
                icon = { section.icon() },
                label = { Text(section.label) },
                alwaysShowLabel = false
            )
        }
        Spacer(Modifier.height(8.dp))
    }
}

@Composable
private fun AdminPlaceholderCard(title: String, subtitle: String) {
    ElevatedCard {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            Text(subtitle, style = MaterialTheme.typography.bodyMedium)
        }
    }
}