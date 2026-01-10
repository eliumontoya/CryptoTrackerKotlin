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
import info.eliumontoyasadec.cryptotracker.domain.model.Fiat
import info.eliumontoyasadec.cryptotracker.domain.repositories.FiatRepository
import info.eliumontoyasadec.cryptotracker.e2e.fakes.FakeFiatRepository
import info.eliumontoyasadec.cryptotracker.e2e.fakes.FakePortfolioRepository
import info.eliumontoyasadec.cryptotracker.e2e.fakes.FakeWalletRepository
import info.eliumontoyasadec.cryptotracker.e2e.fakes.NoOpCryptoRepository
import info.eliumontoyasadec.cryptotracker.e2e.fakes.NoOpHoldingRepository
import info.eliumontoyasadec.cryptotracker.e2e.fakes.NoOpMovementRepository
import info.eliumontoyasadec.cryptotracker.e2e.fakes.NoOpPortfolioQueries
import info.eliumontoyasadec.cryptotracker.e2e.fakes.NoOpTransactionRunner
import info.eliumontoyasadec.cryptotracker.room.db.AppDatabase
import info.eliumontoyasadec.cryptotracker.ui.admin.fiat.AdminFiatScreen
import info.eliumontoyasadec.cryptotracker.ui.admin.fiat.AdminFiatTags
import info.eliumontoyasadec.cryptotracker.ui.shell.AppDeps
import info.eliumontoyasadec.cryptotracker.ui.shell.LocalAppDeps
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class AdminFiatE2eTest {

    @get:org.junit.Rule
    val rule = createAndroidComposeRule<ComponentActivity>()

    private lateinit var fiatRepo: FiatRepository
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
        walletRepo = FakeWalletRepository(seed = emptyList())

        fiatRepo = FakeFiatRepository(
            seed = listOf(
                Fiat(code = "USD", name = "US Dollar", symbol = "$"),
                Fiat(code = "MXN", name = "Peso mexicano", symbol = "$")
            )
        )

        val deps = AppDeps(
            portfolioQueries = NoOpPortfolioQueries(),
            catalogSeeder = CatalogSeeder(db),
            databaseWiper = DatabaseWiper(db),
            portfolioRepository = portfolioRepo,
            walletRepository = walletRepo,
            cryptoRepository = NoOpCryptoRepository(),
            fiatRepository = fiatRepo,
            movementRepository = NoOpMovementRepository(),
            holdingRepository = NoOpHoldingRepository(),
            txRunner = NoOpTransactionRunner()
        )


        rule.setContent {
            CompositionLocalProvider(LocalAppDeps provides deps) {
                AdminFiatScreen()
            }
        }
    }


    @After
    fun tearDown() {
        db.close()
    }

    @Test
    fun showsSeedItems() {
        rule.onNodeWithTag(AdminFiatTags.SCREEN).assertExists()
        rule.onNodeWithTag(AdminFiatTags.LIST).assertExists()

        rule.onNodeWithTag(AdminFiatTags.item("USD")).assertExists()
        rule.onNodeWithTag(AdminFiatTags.item("MXN")).assertExists()
    }

    @Test
    fun createFiat_addsItemToList() {
        rule.onNodeWithTag(AdminFiatTags.FAB_ADD).performClick()

        rule.onNodeWithTag(AdminFiatTags.FORM_DIALOG).assertExists()

        rule.onNodeWithTag(AdminFiatTags.FIELD_CODE).performTextInput("EUR")
        rule.onNodeWithTag(AdminFiatTags.FIELD_NAME).performTextInput("Euro")
        rule.onNodeWithTag(AdminFiatTags.FIELD_SYMBOL).performTextInput("€")

        rule.onNodeWithTag(AdminFiatTags.BTN_SAVE).performClick()

        rule.onNodeWithTag(AdminFiatTags.FORM_DIALOG).assertDoesNotExist()
        rule.onNodeWithTag(AdminFiatTags.item("EUR")).assertExists()
    }

    @Test
    fun editFiat_updatesName() {
        rule.onNodeWithTag(AdminFiatTags.btnEdit("USD")).performClick()

        rule.onNodeWithTag(AdminFiatTags.FORM_DIALOG).assertExists()

        rule.onNodeWithTag(AdminFiatTags.FIELD_NAME).performTextClearance()
        rule.onNodeWithTag(AdminFiatTags.FIELD_NAME).performTextInput("Dolar estadounidense")

        rule.onNodeWithTag(AdminFiatTags.BTN_SAVE).performClick()

        rule.onNodeWithTag(AdminFiatTags.FORM_DIALOG).assertDoesNotExist()

        // valida que dentro del item se vea el nombre actualizado
        rule.onNodeWithTag(AdminFiatTags.item("USD"))
            .assertTextContains("Dolar", substring = true)
    }

    @Test
    fun deleteFiat_removesItem() {
        rule.onNodeWithTag(AdminFiatTags.btnDelete("MXN")).performClick()

        rule.onNodeWithTag(AdminFiatTags.DELETE_DIALOG).assertExists()
        rule.onNodeWithTag(AdminFiatTags.BTN_DELETE_CONFIRM).performClick()

        rule.onNodeWithTag(AdminFiatTags.DELETE_DIALOG).assertDoesNotExist()
        rule.onNodeWithTag(AdminFiatTags.item("MXN")).assertDoesNotExist()
    }


}