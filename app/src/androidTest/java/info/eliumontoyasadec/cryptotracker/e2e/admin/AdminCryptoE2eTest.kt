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
import info.eliumontoyasadec.cryptotracker.domain.repositories.FiatRepository
import info.eliumontoyasadec.cryptotracker.e2e.fakes.FakeCryptoRepository
import info.eliumontoyasadec.cryptotracker.e2e.fakes.FakeFiatRepository
import info.eliumontoyasadec.cryptotracker.e2e.fakes.FakePortfolioRepository
import info.eliumontoyasadec.cryptotracker.e2e.fakes.FakeWalletRepository
import info.eliumontoyasadec.cryptotracker.e2e.fakes.NoOpPortfolioQueries
import info.eliumontoyasadec.cryptotracker.room.db.AppDatabase
import info.eliumontoyasadec.cryptotracker.ui.admin.cryptos.AdminCryptoTags
import info.eliumontoyasadec.cryptotracker.ui.admin.cryptos.AdminCryptosScreen
import info.eliumontoyasadec.cryptotracker.ui.shell.AppDeps
import info.eliumontoyasadec.cryptotracker.ui.shell.LocalAppDeps
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class AdminCryptoE2eTest {

    @get:org.junit.Rule
    val compose = createAndroidComposeRule<ComponentActivity>()

    private lateinit var fiatRepo: FiatRepository
    private lateinit var portfolioRepo: FakePortfolioRepository
    private lateinit var walletRepo: FakeWalletRepository
    private lateinit var cryptoRepo: FakeCryptoRepository

    private lateinit var db: AppDatabase
    @Before
    fun setUp() {
        cryptoRepo = FakeCryptoRepository().apply { seedDefaults() }

        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()


        portfolioRepo = FakePortfolioRepository( )
        walletRepo = FakeWalletRepository( )

        fiatRepo = FakeFiatRepository(

        )

        val deps = AppDeps(
            portfolioQueries = NoOpPortfolioQueries(),
            catalogSeeder = CatalogSeeder(db),
            databaseWiper = DatabaseWiper(db),
            portfolioRepository = portfolioRepo,
            walletRepository = walletRepo,
            cryptoRepository = cryptoRepo,
            fiatRepository = fiatRepo
        )


        compose.setContent {
            CompositionLocalProvider(LocalAppDeps provides deps) {
                AdminCryptosScreen()
            }
        }
    }


    @After
    fun tearDown() {
        db.close()
    }

    @Test
    fun showsSeedItems() {
        compose.onNodeWithTag(AdminCryptoTags.item("BTC"), useUnmergedTree = true).assertExists()
        compose.onNodeWithTag(AdminCryptoTags.item("ETH"), useUnmergedTree = true).assertExists()
    }

    @Test
    fun createCrypto_addsItemToList() {
        compose.onNodeWithTag(AdminCryptoTags.ADD).performClick()

        compose.onNodeWithTag(AdminCryptoTags.SYMBOL_INPUT).performTextInput("XRP")
        compose.onNodeWithTag(AdminCryptoTags.NAME_INPUT).performTextInput("Ripple")

        compose.onNodeWithTag(AdminCryptoTags.SAVE).performClick()

        compose.onNodeWithTag(AdminCryptoTags.item("XRP"), useUnmergedTree = true)
            .assertExists()
            .assertTextContains("Ripple", substring = true)
    }

    @Test
    fun editCrypto_updatesName() {
        compose.onNodeWithTag(AdminCryptoTags.item("BTC"), useUnmergedTree = true).performClick()

        compose.onNodeWithTag(AdminCryptoTags.NAME_INPUT).performTextClearance()
        compose.onNodeWithTag(AdminCryptoTags.NAME_INPUT).performTextInput("Bitcoin Updated")

        compose.onNodeWithTag(AdminCryptoTags.SAVE).performClick()

        compose.onNodeWithTag(AdminCryptoTags.item("BTC"), useUnmergedTree = true)
            .assertExists()
            .assertTextContains("Bitcoin Updated", substring = true)
    }

    @Test
    fun deleteCrypto_removesItem() {
        compose.onNodeWithTag(AdminCryptoTags.delete("SOL"), useUnmergedTree = true).performClick()

        compose.onNodeWithTag(AdminCryptoTags.CONFIRM_DELETE).performClick()

        compose.onNodeWithTag(AdminCryptoTags.item("SOL"), useUnmergedTree = true).assertDoesNotExist()
    }
}