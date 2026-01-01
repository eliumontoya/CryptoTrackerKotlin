package info.eliumontoyasadec.cryptotracker.ui.admin.setup

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Backup
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.UploadFile
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel

@Composable
fun SetupInitialScreen(
    onDeleteAllData: () -> Unit,
    onLoadInitialCatalogs: () -> Unit,
    onLoadInitialMovements: () -> Unit,
    onBackupExport: () -> Unit,
    onBackupImport: () -> Unit
) {
    val vm: SetupInitialViewModel = viewModel(
        factory = SetupInitialViewModelFactory(
            SetupInitialOps(
                deleteAllData = { onDeleteAllData() },
                loadInitialCatalogs = { onLoadInitialCatalogs() },
                loadInitialMovements = { onLoadInitialMovements() },
                backupExport = { onBackupExport() },
                backupImport = { onBackupImport() }
            )
        )
    )

    var state by remember { mutableStateOf(vm.state) }
    LaunchedEffect(vm.state) { state = vm.state }

    Column(
        modifier = Modifier.padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Feedback simple del VM (sin snackbar local; AppShell ya tiene uno si lo quieres)
        if (state.loading) {
            LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
        }

        state.error?.let {
            AssistChip(onClick = {}, label = { Text(it) })
        }

        // Grid 2x2
        Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            SetupCard(
                title = "Eliminar datos existentes",
                subtitle = "Elimina todos los datos almacenados en la aplicación",
                icon = { Icon(Icons.Default.DeleteForever, contentDescription = null) },
                modifier = Modifier.weight(1f),
                onClick = {
                    vm.dispatch(SetupInitialAction.RequestDeleteAll)
                    state = vm.state
                }
            )
            SetupCard(
                title = "Carga de catálogos iniciales",
                subtitle = "Carga catálogos predeterminados de Cryptos, FIAT y Carteras",
                icon = { Icon(Icons.Default.FolderOpen, contentDescription = null) },
                modifier = Modifier.weight(1f),
                onClick = {
                    vm.dispatch(SetupInitialAction.LoadInitialCatalogs)
                    state = vm.state
                }
            )
        }

        Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            SetupCard(
                title = "Carga de movimientos iniciales",
                subtitle = "Importa movimientos iniciales desde un archivo",
                icon = { Icon(Icons.Default.UploadFile, contentDescription = null) },
                modifier = Modifier.weight(1f),
                onClick = {
                    vm.dispatch(SetupInitialAction.LoadInitialMovements)
                    state = vm.state
                }
            )
            SetupCard(
                title = "Realizar y cargar backup",
                subtitle = "Crea copias de seguridad o restaura datos desde un backup",
                icon = { Icon(Icons.Default.Backup, contentDescription = null) },
                modifier = Modifier.weight(1f),
                onClick = {
                    vm.dispatch(SetupInitialAction.BackupExport)
                    state = vm.state
                }
            )
        }

        // Sección Backup
        ElevatedCard {
            Column(
                modifier = Modifier.padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text("Backup", style = MaterialTheme.typography.titleMedium)
                HorizontalDivider()
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(
                        onClick = {
                            vm.dispatch(SetupInitialAction.BackupExport)
                            state = vm.state
                        },
                        enabled = !state.loading
                    ) {
                        Text("Generar backup")
                    }

                    OutlinedButton(
                        onClick = {
                            vm.dispatch(SetupInitialAction.BackupImport)
                            state = vm.state
                        },
                        enabled = !state.loading
                    ) {
                        Text("Cargar backup")
                    }
                }
            }
        }
    }

    // Confirmación de borrado
    if (state.confirmDeleteAll) {
        AlertDialog(
            onDismissRequest = {
                vm.dispatch(SetupInitialAction.CancelDeleteAll)
                state = vm.state
            },
            title = { Text("Confirmar eliminación") },
            text = { Text("Se eliminarán todos los datos de la aplicación. Esta acción no se puede deshacer.") },
            confirmButton = {
                TextButton(onClick = {
                    vm.dispatch(SetupInitialAction.ConfirmDeleteAll)
                    state = vm.state
                }) { Text("Eliminar todo") }
            },
            dismissButton = {
                TextButton(onClick = {
                    vm.dispatch(SetupInitialAction.CancelDeleteAll)
                    state = vm.state
                }) { Text("Cancelar") }
            }
        )
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