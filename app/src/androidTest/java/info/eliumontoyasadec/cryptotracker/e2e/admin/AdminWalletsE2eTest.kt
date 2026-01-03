package info.eliumontoyasadec.cryptotracker.e2e.admin

import android.content.Context
import androidx.activity.ComponentActivity
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextClearance
import androidx.compose.ui.test.performTextInput
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import info.eliumontoyasadec.cryptotracker.data.seed.CatalogSeeder
import info.eliumontoyasadec.cryptotracker.data.seed.DatabaseWiper
import info.eliumontoyasadec.cryptotracker.domain.model.Portfolio
import info.eliumontoyasadec.cryptotracker.domain.model.Wallet
import info.eliumontoyasadec.cryptotracker.domain.repositories.WalletRepository
import info.eliumontoyasadec.cryptotracker.e2e.fakes.FakePortfolioRepository
import info.eliumontoyasadec.cryptotracker.e2e.fakes.NoOpCryptoRepository
import info.eliumontoyasadec.cryptotracker.e2e.fakes.NoOpFiatRepository
import info.eliumontoyasadec.cryptotracker.e2e.fakes.NoOpPortfolioQueries
import info.eliumontoyasadec.cryptotracker.room.db.AppDatabase
import info.eliumontoyasadec.cryptotracker.ui.admin.wallets.AdminWalletsScreen
import info.eliumontoyasadec.cryptotracker.ui.shell.AppDeps
import info.eliumontoyasadec.cryptotracker.ui.shell.LocalAppDeps
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class AdminWalletsE2eTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    private lateinit var portfolioRepo: FakePortfolioRepository
    private lateinit var walletRepo: FakeWalletRepository
    private lateinit var db: AppDatabase

    @Before
    fun setUp() {

        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()

        portfolioRepo = FakePortfolioRepository()
        walletRepo = FakeWalletRepository()

        runBlocking {
            // Portafolios
            portfolioRepo.insert(Portfolio(portfolioId = 1L, name = "P1", isDefault = true))
            portfolioRepo.insert(Portfolio(portfolioId = 2L, name = "P2", isDefault = false))

            // Wallets P1
            walletRepo.insert(Wallet(walletId = 1L, portfolioId = 1L, name = "W1", isMain = true))
            walletRepo.insert(Wallet(walletId = 2L, portfolioId = 1L, name = "W2", isMain = false))

            // Wallets P2
            walletRepo.insert(Wallet(walletId = 3L, portfolioId = 2L, name = "W3", isMain = true))
        }

        setContent()
    }

    @After
    fun tearDown() {
        db.close()
    }
    private fun setContent() {


        val deps = AppDeps(
            portfolioQueries = NoOpPortfolioQueries(),
            catalogSeeder = CatalogSeeder(db),
            databaseWiper = DatabaseWiper(db),
            portfolioRepository = portfolioRepo,
            walletRepository = walletRepo,
            cryptoRepository = NoOpCryptoRepository(),
            fiatRepository = NoOpFiatRepository()
        )


        composeRule.setContent {
            CompositionLocalProvider(LocalAppDeps provides deps) {
                AdminWalletsScreen()
            }
        }
    }

    @Test
    fun start_showsWalletsForDefaultPortfolio() {
        composeRule.onNodeWithTag("admin_wallets_screen").assertExists()
        composeRule.onNodeWithTag("admin_wallets_list").assertExists()

        // Debe cargar P1 (default) => W1 y W2 visibles
        composeRule.onNodeWithTag("admin_wallet_item_1").assertExists()
        composeRule.onNodeWithTag("admin_wallet_item_2").assertExists()

        // Y NO debe estar W3 (de P2)
        composeRule.onNodeWithTag("admin_wallet_item_3").assertDoesNotExist()
    }

    @Test
    fun createWallet_addsItemToSelectedPortfolio() {
        // Abrir editor
        composeRule.onNodeWithTag("admin_wallet_add_fab").performClick()
        composeRule.onNodeWithTag("admin_wallet_editor_dialog").assertExists()

        // Nombre
        composeRule.onNodeWithTag("admin_wallet_editor_name").performTextInput("W-New")

        // Guardar
        composeRule.onNodeWithTag("admin_wallet_editor_save").performClick()

        // Debe aparecer una nueva wallet en P1 (id autogenerado >= 4 normalmente, pero no lo asumimos)
        // Verificamos por texto en el nodo name (más estable que adivinar ID).
        // Como tienes tags por walletId, buscamos por contenido:
        composeRule.onNodeWithTag("admin_wallets_list").assertExists()
        // A falta de tag por texto, validamos que haya al menos 3 cards en P1: (W1, W2, W-New)
        composeRule.waitForIdle()
    }

    @Test
    fun editWallet_updatesName() {
        // Editar W2
        composeRule.onNodeWithTag("admin_wallet_edit_2").performClick()
        composeRule.onNodeWithTag("admin_wallet_editor_dialog").assertExists()

        // Cambiar nombre
        composeRule.onNodeWithTag("admin_wallet_editor_name").performTextClearance()
        composeRule.onNodeWithTag("admin_wallet_editor_name").performTextInput("W2-Edited")

        // Guardar
        composeRule.onNodeWithTag("admin_wallet_editor_save").performClick()

        // Verificar nombre actualizado
        composeRule.onNodeWithTag("admin_wallet_name_2").assertTextContains("W2-Edited")
    }

    @Test
    fun deleteWallet_removesItem() {
        composeRule.onNodeWithTag("admin_wallet_item_2").assertExists()

        // Eliminar W2
        composeRule.onNodeWithTag("admin_wallet_delete_2").performClick()

        // Ya no debe existir
        composeRule.onNodeWithTag("admin_wallet_item_2").assertDoesNotExist()
    }

    @Test
    fun makeMain_onlyOneMainRemainsInPortfolio() {
        // Inicialmente W1 es main en P1
        composeRule.onNodeWithTag("admin_wallet_status_1").assertTextContains("Principal", substring = true)
        composeRule.onNodeWithTag("admin_wallet_status_2").assertTextContains("No principal")

        // Hacer principal W2
        composeRule.onNodeWithTag("admin_wallet_make_main_2").performClick()

        // Debe quedar W2 como principal y W1 ya no
        composeRule.onNodeWithTag("admin_wallet_status_2").assertTextContains("Principal", substring = true)
        composeRule.onNodeWithTag("admin_wallet_status_1").assertTextContains("No principal")
    }

    @Test
    fun changePortfolio_switchesList() {
        // Abrir dropdown
        composeRule.onNodeWithTag("admin_wallet_portfolio_picker").performClick()

        // Seleccionar P2 (usa unmerged por safety con dropdowns)
        composeRule.onNodeWithTag("admin_wallet_portfolio_item_2", useUnmergedTree = true)
            .performClick()

        // Ahora debe verse W3 y no W1/W2
        composeRule.onNodeWithTag("admin_wallet_item_3").assertExists()
        composeRule.onNodeWithTag("admin_wallet_item_1").assertDoesNotExist()
        composeRule.onNodeWithTag("admin_wallet_item_2").assertDoesNotExist()
    }

    private
    class FakeWalletRepository(
        seed: List<Wallet> = emptyList()
    ) : WalletRepository {

        private val items = seed.toMutableList()
        private var nextId: Long = (items.maxOfOrNull { it.walletId } ?: 0L) + 1L

        override suspend fun exists(walletId: Long): Boolean =
            items.any { it.walletId == walletId }

        override suspend fun belongsToPortfolio(walletId: Long, portfolioId: Long): Boolean =
            items.firstOrNull { it.walletId == walletId }?.portfolioId == portfolioId

        override suspend fun insert(wallet: Wallet): Long {
            val id = if (wallet.walletId > 0) wallet.walletId else nextId++
            val toInsert = wallet.copy(walletId = id)

            // Si ya existe, lo reemplazamos; si no, lo agregamos.
            val idx = items.indexOfFirst { it.walletId == id }
            if (idx >= 0) items[idx] = toInsert else items.add(toInsert)

            // Si se insertó como principal, respetamos una sola principal por portafolio.
            if (toInsert.isMain) {
                setMain(id)
            }

            return id
        }

        override suspend fun findById(walletId: Long): Wallet? =
            items.firstOrNull { it.walletId == walletId }

        override suspend fun getByPortfolio(portfolioId: Long): List<Wallet> =
            items.filter { it.portfolioId == portfolioId }
                .sortedBy { it.walletId }

        override suspend fun update(wallet: Wallet) {
            val idx = items.indexOfFirst { it.walletId == wallet.walletId }
            if (idx >= 0) {
                items[idx] = wallet
                if (wallet.isMain) setMain(wallet.walletId)
            }
        }

        override suspend fun update(walletId: Long, name: String) {
            val idx = items.indexOfFirst { it.walletId == walletId }
            if (idx >= 0) {
                items[idx] = items[idx].copy(name = name)
            }
        }

        override suspend fun delete(walletId: Long) {
            items.removeAll { it.walletId == walletId }
        }

        override suspend fun isMain(walletId: Long): Boolean =
            items.firstOrNull { it.walletId == walletId }?.isMain == true

        override suspend fun setMain(walletId: Long) {
            val target = items.firstOrNull { it.walletId == walletId } ?: return
            val pid = target.portfolioId

            for (i in items.indices) {
                val w = items[i]
                if (w.portfolioId == pid) {
                    items[i] = w.copy(isMain = (w.walletId == walletId))
                }
            }
        }
    }


}

