package info.eliumontoyasadec.cryptotracker.e2e.movements

import android.content.Context
import androidx.activity.ComponentActivity
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextClearance
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.performTextReplacement
import androidx.compose.ui.test.printToLog
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import info.eliumontoyasadec.cryptotracker.data.seed.CatalogSeeder
import info.eliumontoyasadec.cryptotracker.data.seed.DatabaseWiper
import info.eliumontoyasadec.cryptotracker.e2e.fakes.FakeCryptoRepository
import info.eliumontoyasadec.cryptotracker.e2e.fakes.FakeFiatRepository
import info.eliumontoyasadec.cryptotracker.e2e.fakes.FakePortfolioRepository
import info.eliumontoyasadec.cryptotracker.e2e.fakes.FakeWalletRepository
import info.eliumontoyasadec.cryptotracker.e2e.fakes.NoOpHoldingRepository
import info.eliumontoyasadec.cryptotracker.e2e.fakes.NoOpMovementRepository
import info.eliumontoyasadec.cryptotracker.e2e.fakes.NoOpPortfolioQueries
import info.eliumontoyasadec.cryptotracker.e2e.fakes.NoOpTransactionRunner
import info.eliumontoyasadec.cryptotracker.room.db.AppDatabase
import info.eliumontoyasadec.cryptotracker.ui.screens.movements.MovementTags
import info.eliumontoyasadec.cryptotracker.ui.shell.AppDeps
import info.eliumontoyasadec.cryptotracker.ui.shell.AppShell
import info.eliumontoyasadec.cryptotracker.ui.shell.LocalAppDeps
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class MovementsInE2eTest {

    @get:org.junit.Rule
    val compose = createAndroidComposeRule<ComponentActivity>()

    private lateinit var db: AppDatabase

    private lateinit var portfolioRepo: FakePortfolioRepository
    private lateinit var walletRepo: FakeWalletRepository
    private lateinit var cryptoRepo: FakeCryptoRepository
    private lateinit var fiatRepo: FakeFiatRepository

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()

        // Repos fake/noop (aunque Movements aún no los use)
        portfolioRepo = FakePortfolioRepository()
        walletRepo = FakeWalletRepository()
        cryptoRepo = FakeCryptoRepository().apply { seedDefaults() }
        fiatRepo = FakeFiatRepository()

        val deps = AppDeps(
            portfolioQueries = NoOpPortfolioQueries(),
            catalogSeeder = CatalogSeeder(db),
            databaseWiper = DatabaseWiper(db),
            portfolioRepository = portfolioRepo,
            walletRepository = walletRepo,
            cryptoRepository = cryptoRepo,
            fiatRepository = fiatRepo,

            movementRepository = NoOpMovementRepository(),
            holdingRepository = NoOpHoldingRepository(),
            txRunner = NoOpTransactionRunner()
        )

        compose.setContent {
            CompositionLocalProvider(LocalAppDeps provides deps) {
                AppShell()
            }
        }

        // Navegación real: abrir drawer y entrar a "Entrada"
        compose.onNodeWithContentDescription("Menú").performClick()
        compose.onNodeWithText("Entrada").performClick()

        // Confirmamos que ya estamos en el screen objetivo
        compose.onNodeWithTag(MovementTags.Screen).assertExists()
    }

    @After
    fun tearDown() {
        db.close()
    }

    @Test
    fun createMovement_addsItem() {
        val qty = "%.2f".format(java.util.Locale.US, kotlin.random.Random.nextDouble(0.01, 99.99))

        val prc = "%.2f".format(java.util.Locale.US, kotlin.random.Random.nextDouble(0.01, 999.99))

        compose.onNodeWithTag(MovementTags.AddButton).performClick()

        // Form visible
        compose.onNodeWithTag(MovementTags.FormSheet).assertExists()
        compose.onNodeWithTag(MovementTags.FormSave).assertIsNotEnabled()


        // Cambia cantidad
        compose.onNodeWithTag(MovementTags.FormQuantity).performTextReplacement(qty)



        // Precio (opcional, pero lo llenamos para forzar recomposición/validación)
        compose.onNodeWithTag(MovementTags.FormPrice)
            .performTextReplacement(prc)



        // Guardar
        compose.onNodeWithTag(MovementTags.FormSave).assertIsEnabled()

        compose.onNodeWithTag(MovementTags.FormSave).performClick()


        // Verificación simple: aparece el headline esperado en IN mode
        // (usa texto como “oráculo” porque el id es generado por timestamp)
        compose.onNodeWithText(qty, substring = true).assertExists()
        compose.onNodeWithText(prc, substring = true).assertExists()

    }

    @Test
    fun editSeedMovement_updatesHeadline() {
        // Usamos un row seed conocido por fakeRowsFor(IN): "in-1"
        val id = "in-1"

        val qty = "%.2f".format(java.util.Locale.US, kotlin.random.Random.nextDouble(0.01, 99.99))

        compose.onNodeWithTag(MovementTags.rowMenu(id)).performClick()
        compose.onNodeWithTag(MovementTags.rowEdit(id)).performClick()

        compose.onNodeWithTag(MovementTags.FormQuantity).performTextClearance()
        compose.onNodeWithTag(MovementTags.FormQuantity).performTextInput(qty)
        compose.onNodeWithTag(MovementTags.FormSave).performClick()

        //compose.onRoot().printToLog("MOVEMENTS_TREE")
        compose.onNodeWithTag(MovementTags.List).printToLog("MOVEMENTS_LIST")

        compose.onNodeWithText(qty, substring = true).assertExists()

    }



    @Test
    fun deleteSeedMovement_removesRow() {
        // Seed row "in-2"
        val id = "in-2"

        compose.onNodeWithTag(MovementTags.rowMenu(id)).performClick()
        compose.onNodeWithTag(MovementTags.rowDelete(id)).performClick()

        compose.onNodeWithTag(MovementTags.DeleteDialog).assertExists()
        compose.onNodeWithTag(MovementTags.DeleteConfirm).performClick()

        compose.onNodeWithTag(MovementTags.row(id)).assertDoesNotExist()
    }
}