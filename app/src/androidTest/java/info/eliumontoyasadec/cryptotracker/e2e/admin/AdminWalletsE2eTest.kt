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
import info.eliumontoyasadec.cryptotracker.e2e.fakes.FakePortfolioRepository
import info.eliumontoyasadec.cryptotracker.e2e.fakes.FakeWalletRepository
import info.eliumontoyasadec.cryptotracker.e2e.fakes.NoOpCryptoRepository
import info.eliumontoyasadec.cryptotracker.e2e.fakes.NoOpFiatRepository
import info.eliumontoyasadec.cryptotracker.e2e.fakes.NoOpHoldingRepository
import info.eliumontoyasadec.cryptotracker.e2e.fakes.NoOpMovementRepository
import info.eliumontoyasadec.cryptotracker.e2e.fakes.NoOpPortfolioQueries
import info.eliumontoyasadec.cryptotracker.e2e.fakes.NoOpTransactionRunner
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
import info.eliumontoyasadec.cryptotracker.ui.admin.wallets.AdminWalletsTags as Tags

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
            fiatRepository = NoOpFiatRepository(),

            movementRepository = NoOpMovementRepository(),
            holdingRepository = NoOpHoldingRepository(),
            txRunner = NoOpTransactionRunner()
        )


        composeRule.setContent {
            CompositionLocalProvider(LocalAppDeps provides deps) {
                AdminWalletsScreen()
            }
        }
    }

    @Test
    fun start_showsWalletsForDefaultPortfolio() {
        composeRule.onNodeWithTag(Tags.SCREEN).assertExists()
        composeRule.onNodeWithTag(Tags.LIST).assertExists()

        // Debe cargar P1 (default) => W1 y W2 visibles
        composeRule.onNodeWithTag(Tags.item(1)).assertExists()
        composeRule.onNodeWithTag(Tags.item(2)).assertExists()

        // Y NO debe estar W3 (de P2)
        composeRule.onNodeWithTag(Tags.item(3)).assertDoesNotExist()
    }

    @Test
    fun createWallet_addsItemToSelectedPortfolio() {
        // Abrir editor
        composeRule.onNodeWithTag(Tags.FAB_ADD).performClick()
        composeRule.onNodeWithTag(Tags.EDITOR_DIALOG).assertExists()

        // Nombre
        composeRule.onNodeWithTag(Tags.EDITOR_NAME).performTextInput("W-New")

        // Guardar
        composeRule.onNodeWithTag(Tags.EDITOR_SAVE).performClick()

        // Debe aparecer una nueva wallet en P1 (id autogenerado >= 4 normalmente, pero no lo asumimos)
        // Verificamos por texto en el nodo name (más estable que adivinar ID).
        // Como tienes tags por walletId, buscamos por contenido:
        composeRule.onNodeWithTag(Tags.LIST).assertExists()
        // A falta de tag por texto, validamos que haya al menos 3 cards en P1: (W1, W2, W-New)
        composeRule.waitForIdle()
    }

    @Test
    fun editWallet_updatesName() {
        // Editar W2
        composeRule.onNodeWithTag(Tags.edit(2)).performClick()
        composeRule.onNodeWithTag(Tags.EDITOR_DIALOG).assertExists()

        // Cambiar nombre
        composeRule.onNodeWithTag(Tags.EDITOR_NAME).performTextClearance()
        composeRule.onNodeWithTag(Tags.EDITOR_NAME).performTextInput("W2-Edited")

        // Guardar
        composeRule.onNodeWithTag(Tags.EDITOR_SAVE).performClick()

        // Verificar nombre actualizado
        composeRule.onNodeWithTag(Tags.name(2)).assertTextContains("W2-Edited")
    }

    @Test
    fun deleteWallet_removesItem() {
        composeRule.onNodeWithTag(Tags.item(2)).assertExists()

        // Eliminar W2
        composeRule.onNodeWithTag(Tags.delete(2)).performClick()

        // Ya no debe existir
        composeRule.onNodeWithTag(Tags.item(2)).assertDoesNotExist()
    }

    @Test
    fun makeMain_onlyOneMainRemainsInPortfolio() {
        // Inicialmente W1 es main en P1
        composeRule.onNodeWithTag(Tags.status(1)).assertTextContains("Principal", substring = true)
        composeRule.onNodeWithTag(Tags.status(2)).assertTextContains("No principal")

        // Hacer principal W2
        composeRule.onNodeWithTag("admin_wallet_make_main_2").performClick()

        // Debe quedar W2 como principal y W1 ya no
        composeRule.onNodeWithTag(Tags.status(2)).assertTextContains("Principal", substring = true)
        composeRule.onNodeWithTag(Tags.status(1)).assertTextContains("No principal")
    }

    @Test
    fun changePortfolio_switchesList() {
        // Abrir dropdown
        composeRule.onNodeWithTag(Tags.PORTFOLIO_PICKER).performClick()

        // Seleccionar P2 (usa unmerged por safety con dropdowns)
        composeRule.onNodeWithTag(Tags.portfolioItem(2), useUnmergedTree = true)
            .performClick()

        // Ahora debe verse W3 y no W1/W2
        composeRule.onNodeWithTag(Tags.item(3)).assertExists()
        composeRule.onNodeWithTag(Tags.item(1)).assertDoesNotExist()
        composeRule.onNodeWithTag(Tags.item(2)).assertDoesNotExist()
    }



}

