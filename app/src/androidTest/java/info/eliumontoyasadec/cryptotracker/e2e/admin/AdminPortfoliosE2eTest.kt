package info.eliumontoyasadec.cryptotracker.e2e.admin

import android.content.Context
import androidx.activity.ComponentActivity
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextClearance
import androidx.compose.ui.test.performTextInput
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import info.eliumontoyasadec.cryptotracker.data.seed.CatalogSeeder
import info.eliumontoyasadec.cryptotracker.data.seed.DatabaseWiper
import info.eliumontoyasadec.cryptotracker.e2e.fakes.FakePortfolioRepository
import info.eliumontoyasadec.cryptotracker.e2e.fakes.NoOpCryptoRepository
import info.eliumontoyasadec.cryptotracker.e2e.fakes.NoOpFiatRepository
import info.eliumontoyasadec.cryptotracker.e2e.fakes.NoOpPortfolioQueries
import info.eliumontoyasadec.cryptotracker.e2e.fakes.NoOpWalletRepository
import info.eliumontoyasadec.cryptotracker.room.db.AppDatabase
import info.eliumontoyasadec.cryptotracker.ui.admin.portfolios.AdminPortfoliosScreen
import info.eliumontoyasadec.cryptotracker.ui.shell.AppDeps
import info.eliumontoyasadec.cryptotracker.ui.shell.LocalAppDeps
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test

/**
 * E2E instrumented tests for AdminPortfoliosScreen:
 * VM real + UseCases reales + PortfolioRepository fake in-memory.
 *
 * Nota: CatalogSeeder/DatabaseWiper requieren AppDatabase, así que se crea un Room in-memory
 * únicamente para poder construir esas dependencias (no se usa para portfolios).
 *
 * Requiere que AdminPortfoliosScreen tenga testTags:
 * - admin_portfolios_add_fab
 * - admin_portfolios_form_name
 * - admin_portfolios_form_save
 * - admin_portfolios_delete_<id>
 * - admin_portfolios_make_default_<id>
 * - admin_portfolios_default_badge_<id>
 */
class AdminPortfoliosE2eTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    private lateinit var db: AppDatabase

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
    }

    @After
    fun tearDown() {
        db.close()
    }

    private fun setScreen(repo: FakePortfolioRepository) {
        val deps = AppDeps(
            portfolioQueries = NoOpPortfolioQueries(),
            catalogSeeder = CatalogSeeder(db),
            databaseWiper = DatabaseWiper(db),
            portfolioRepository = repo,
            walletRepository = NoOpWalletRepository(),
            cryptoRepository = NoOpCryptoRepository(),
            fiatRepository = NoOpFiatRepository()
        )

        composeRule.setContent {
            CompositionLocalProvider(LocalAppDeps provides deps) {
                AdminPortfoliosScreen()
            }
        }
    }

    @Test
    fun createPortfolio_appearsInList() {
        val repo = FakePortfolioRepository()
        setScreen(repo)

        // Click +
        composeRule.onNodeWithTag("admin_portfolios_add_fab").performClick()

        // Fill name
        composeRule.onNodeWithTag("admin_portfolios_form_name")
            .performTextInput("Principal")

        // Save
        composeRule.onNodeWithTag("admin_portfolios_form_save").performClick()

        // Assert appears
        composeRule.onNodeWithText("Principal").assertExists()
    }

    @Test
    fun deletePortfolio_disappearsFromList() {
        val repo = FakePortfolioRepository()
        setScreen(repo)

        // Create first portfolio (id=1 in FakePortfolioRepository)
        composeRule.onNodeWithTag("admin_portfolios_add_fab").performClick()
        composeRule.onNodeWithTag("admin_portfolios_form_name").performTextInput("BorrarYo")
        composeRule.onNodeWithTag("admin_portfolios_form_save").performClick()
        composeRule.onNodeWithText("BorrarYo").assertExists()

        // Delete by tag (first insert => id=1)
        composeRule.onNodeWithTag("admin_portfolios_delete_1").performClick()

        // Confirmation dialog (si tu UI usa otro texto, cámbialo aquí)
        composeRule.onNodeWithText("Eliminar").performClick()

        // Assert gone
        composeRule.onNodeWithText("BorrarYo").assertDoesNotExist()
    }

    @Test
    fun setDefault_onlyOneDefaultRemains() {
        val repo = FakePortfolioRepository()
        setScreen(repo)

        // Create P1 (id=1)
        composeRule.onNodeWithTag("admin_portfolios_add_fab").performClick()
        composeRule.onNodeWithTag("admin_portfolios_form_name").performTextInput("P1")
        composeRule.onNodeWithTag("admin_portfolios_form_save").performClick()
        composeRule.onNodeWithText("P1").assertExists()

        // Create P2 (id=2)
        composeRule.onNodeWithTag("admin_portfolios_add_fab").performClick()
        composeRule.onNodeWithTag("admin_portfolios_form_name").performTextClearance()
        composeRule.onNodeWithTag("admin_portfolios_form_name").performTextInput("P2")
        composeRule.onNodeWithTag("admin_portfolios_form_save").performClick()
        composeRule.onNodeWithText("P2").assertExists()

        // Make P2 default
        composeRule.onNodeWithTag("admin_portfolios_make_default_2").performClick()

        // Assert only P2 has default badge
        composeRule.onNodeWithTag("admin_portfolios_default_badge_2", useUnmergedTree = true).assertExists()
        composeRule.onNodeWithTag("admin_portfolios_default_badge_1", useUnmergedTree = true).assertDoesNotExist()
    }
}