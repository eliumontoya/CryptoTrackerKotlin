package info.eliumontoyasadec.cryptotracker.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Divider
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

// -------- Domain-ish UI models (still fake) --------

enum class MovementMode { IN, OUT, BETWEEN, SWAP }

enum class WalletFilter(val label: String) {
    ALL("Todas"),
    METAMASK("Metamask"),
    BYBIT("ByBit"),
    PHANTOM("Phantom")
}

enum class CryptoFilter(val label: String) {
    ALL("Todas"),
    BTC("BTC"),
    ETH("ETH"),
    SOL("SOL"),
    ALGO("ALGO"),
    AIXBT("AIXBT")
}

data class MovementRow(
    val id: String,
    val dateLabel: String,
    val wallet: WalletFilter,
    val crypto: CryptoFilter,
    val headline: String,
    val details: String
)

// -------- UI State --------

data class MovementsUiState(
    val selectedWallet: WalletFilter = WalletFilter.ALL,
    val selectedCrypto: CryptoFilter = CryptoFilter.ALL,
    val rows: List<MovementRow> = emptyList(),
    val filteredRows: List<MovementRow> = emptyList()
)

// -------- Composable --------

@Composable
fun MovementsScreen(
    title: String,
    state: MovementsUiState,
    onSelectWallet: (WalletFilter) -> Unit,
    onSelectCrypto: (CryptoFilter) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(title, style = MaterialTheme.typography.headlineSmall)

        // Filters
        ElevatedCard {
            Column(
                modifier = Modifier.padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text("Filtros", style = MaterialTheme.typography.titleMedium)
                Divider()

                Text("Cartera", style = MaterialTheme.typography.labelLarge)
                ChipRow(
                    options = WalletFilter.entries,
                    selected = state.selectedWallet,
                    labelOf = { it.label },
                    onSelect = onSelectWallet
                )

                Spacer(Modifier.height(6.dp))

                Text("Crypto", style = MaterialTheme.typography.labelLarge)
                ChipRow(
                    options = CryptoFilter.entries,
                    selected = state.selectedCrypto,
                    labelOf = { it.label },
                    onSelect = onSelectCrypto
                )
            }
        }

        // List
        ElevatedCard {
            Column(
                modifier = Modifier.padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Movimientos", style = MaterialTheme.typography.titleMedium)
                    Text("${state.filteredRows.size}", style = MaterialTheme.typography.labelLarge)
                }
                Divider()

                if (state.filteredRows.isEmpty()) {
                    Text(
                        text = "No hay movimientos con los filtros actuales.",
                        style = MaterialTheme.typography.bodyMedium
                    )
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(state.filteredRows, key = { it.id }) { row ->
                            MovementRowItem(row)
                            Divider()
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun <T> ChipRow(
    options: List<T>,
    selected: T,
    labelOf: (T) -> String,
    onSelect: (T) -> Unit
) {
    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        items(options) { opt ->
            FilterChip(
                selected = opt == selected,
                onClick = { onSelect(opt) },
                label = { Text(labelOf(opt)) }
            )
        }
    }
}

@Composable
private fun MovementRowItem(row: MovementRow) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(row.headline, style = MaterialTheme.typography.titleSmall)
            Text(row.dateLabel, style = MaterialTheme.typography.labelMedium)
        }
        Text(
            text = "${row.wallet.label} · ${row.crypto.label}",
            style = MaterialTheme.typography.bodySmall
        )
        Text(row.details, style = MaterialTheme.typography.bodyMedium)
    }
}

// -------- ViewModel (fake data) --------

class MovementsViewModel(private val mode: MovementMode) : ViewModel() {

    private val allRows: List<MovementRow> = fakeRowsFor(mode)

    private val _state = MutableStateFlow(
        MovementsUiState(
            selectedWallet = WalletFilter.ALL,
            selectedCrypto = CryptoFilter.ALL,
            rows = allRows,
            filteredRows = allRows
        )
    )
    val state: StateFlow<MovementsUiState> = _state.asStateFlow()

    fun selectWallet(wallet: WalletFilter) {
        val next = _state.value.copy(selectedWallet = wallet)
        _state.value = next.copy(filteredRows = applyFilters(next))
    }

    fun selectCrypto(crypto: CryptoFilter) {
        val next = _state.value.copy(selectedCrypto = crypto)
        _state.value = next.copy(filteredRows = applyFilters(next))
    }

    private fun applyFilters(state: MovementsUiState): List<MovementRow> {
        return state.rows.filter { row ->
            (state.selectedWallet == WalletFilter.ALL || row.wallet == state.selectedWallet) &&
                (state.selectedCrypto == CryptoFilter.ALL || row.crypto == state.selectedCrypto)
        }
    }

    class Factory(private val mode: MovementMode) : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(MovementsViewModel::class.java)) {
                @Suppress("UNCHECKED_CAST")
                return MovementsViewModel(mode) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class")
        }
    }
}

// -------- Fake data --------

private fun fakeRowsFor(mode: MovementMode): List<MovementRow> {
    return when (mode) {
        MovementMode.IN -> listOf(
            MovementRow(
                id = "in-1",
                dateLabel = "15 Feb 2025",
                wallet = WalletFilter.METAMASK,
                crypto = CryptoFilter.BTC,
                headline = "+ 0.10 BTC",
                details = "Entrada a Metamask (fake)"
            ),
            MovementRow(
                id = "in-2",
                dateLabel = "12 Feb 2025",
                wallet = WalletFilter.BYBIT,
                crypto = CryptoFilter.AIXBT,
                headline = "+ 12 AIXBT",
                details = "Entrada a ByBit (fake)"
            )
        )

        MovementMode.OUT -> listOf(
            MovementRow(
                id = "out-1",
                dateLabel = "16 Feb 2025",
                wallet = WalletFilter.METAMASK,
                crypto = CryptoFilter.BTC,
                headline = "- 0.05 BTC",
                details = "Salida desde Metamask (fake)"
            ),
            MovementRow(
                id = "out-2",
                dateLabel = "13 Feb 2025",
                wallet = WalletFilter.PHANTOM,
                crypto = CryptoFilter.SOL,
                headline = "- 1 SOL",
                details = "Salida desde Phantom (fake)"
            )
        )

        MovementMode.BETWEEN -> listOf(
            MovementRow(
                id = "btw-1",
                dateLabel = "14 Feb 2025",
                wallet = WalletFilter.METAMASK,
                crypto = CryptoFilter.ALGO,
                headline = "Metamask → Phantom",
                details = "3 ALGO transferidos (fake)"
            ),
            MovementRow(
                id = "btw-2",
                dateLabel = "12 Feb 2025",
                wallet = WalletFilter.BYBIT,
                crypto = CryptoFilter.AIXBT,
                headline = "ByBit → Metamask",
                details = "2 AIXBT transferidos (fake)"
            )
        )

        MovementMode.SWAP -> listOf(
            MovementRow(
                id = "sw-1",
                dateLabel = "13 Feb 2025",
                wallet = WalletFilter.METAMASK,
                crypto = CryptoFilter.AIXBT,
                headline = "Swap en Metamask",
                details = "+ 1 AIXBT · - 5 ALGO (fake)"
            ),
            MovementRow(
                id = "sw-2",
                dateLabel = "11 Feb 2025",
                wallet = WalletFilter.PHANTOM,
                crypto = CryptoFilter.SOL,
                headline = "Swap en Phantom",
                details = "+ 2 SOL · - 0.01 BTC (fake)"
            )
        )
    }
}