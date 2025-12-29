package info.eliumontoyasadec.cryptotracker.ui.admin

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun SetupInitialScreen(
    onDeleteAllData: () -> Unit,
    onLoadInitialCatalogs: () -> Unit,
    onLoadInitialMovements: () -> Unit,
    onBackupExport: () -> Unit,
    onBackupImport: () -> Unit
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Header similar a PortfolioHeader
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text("Configuración inicial", style = MaterialTheme.typography.headlineSmall)
            Text("Selecciona una opción para comenzar", style = MaterialTheme.typography.bodySmall)
        }

        // 4 tarjetas (como tu captura)
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
            SetupCard(
                title = "Eliminar datos existentes",
                subtitle = "Elimina todos los datos almacenados en la aplicación",
                icon = { Icon(Icons.Default.DeleteForever, contentDescription = null) },
                modifier = Modifier.weight(1f),
                onClick = onDeleteAllData
            )
            SetupCard(
                title = "Carga de catálogos iniciales",
                subtitle = "Carga catálogos predeterminados de Cryptos, FIAT y Carteras",
                icon = { Icon(Icons.Default.FolderOpen, contentDescription = null) },
                modifier = Modifier.weight(1f),
                onClick = onLoadInitialCatalogs
            )
        }

        Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
            SetupCard(
                title = "Carga de movimientos iniciales",
                subtitle = "Importa movimientos iniciales desde un archivo",
                icon = { Icon(Icons.Default.UploadFile, contentDescription = null) },
                modifier = Modifier.weight(1f),
                onClick = onLoadInitialMovements
            )
            SetupCard(
                title = "Realizar y cargar backup",
                subtitle = "Crea copias de seguridad o restaura datos desde un backup",
                icon = { Icon(Icons.Default.Backup, contentDescription = null) },
                modifier = Modifier.weight(1f),
                onClick = {
                    // Por ahora: acción genérica. Más adelante lo separamos a Export/Import
                    onBackupExport()
                }
            )
        }

        // Opcional: acciones separadas (si quieres igual que “export/import”)
        ElevatedCard {
            Column(
                modifier = Modifier.padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text("Backup", style = MaterialTheme.typography.titleMedium)
                Divider()
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick = onBackupExport) { Text("Generar backup") }
                    OutlinedButton(onClick = onBackupImport) { Text("Cargar backup") }
                }
            }
        }
    }
}

@Composable
private fun SetupCard(
    title: String,
    subtitle: String,
    icon: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    ElevatedCard(
        modifier = modifier,
        onClick = onClick
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Ícono “circular” como estilo de dashboard
            Surface(
                shape = MaterialTheme.shapes.extraLarge,
                tonalElevation = 2.dp
            ) {
                Box(Modifier.padding(10.dp)) { icon() }
            }

            Text(title, style = MaterialTheme.typography.titleMedium)
            Text(subtitle, style = MaterialTheme.typography.bodySmall)
        }
    }
}