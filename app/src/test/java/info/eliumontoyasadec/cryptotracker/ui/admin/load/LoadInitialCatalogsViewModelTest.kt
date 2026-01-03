package info.eliumontoyasadec.cryptotracker.ui.admin.load

import info.eliumontoyasadec.cryptotracker.data.seed.CatalogSeeder
import info.eliumontoyasadec.cryptotracker.data.seed.CatalogStatus
import info.eliumontoyasadec.cryptotracker.data.seed.SeedRequest
import info.eliumontoyasadec.cryptotracker.data.seed.SeedResult
import info.eliumontoyasadec.cryptotracker.ui.admin.MainDispatcherRule
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class LoadInitialCatalogsViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val seeder: CatalogSeeder = mockk()

    private fun status(
        portfolios: Int = 0,
        wallets: Int = 0,
        cryptos: Int = 0,
        fiat: Int = 0
    ) = CatalogStatus(
        portfolios = portfolios,
        wallets = wallets,
        cryptos = cryptos,
        fiat = fiat
    )

    @Test
    fun `init - refreshStatus updates already flags when counts are greater than 0`() = runTest {
        coEvery { seeder.status() } returns status(wallets = 1, cryptos = 0, fiat = 2)

        val vm = LoadInitialCatalogsViewModel(seeder)
        advanceUntilIdle()

        assertNull(vm.state.error)
        assertTrue(vm.state.walletsAlready)
        assertFalse(vm.state.cryptosAlready)
        assertTrue(vm.state.fiatAlready)

        coVerify(exactly = 1) { seeder.status() }
    }

    @Test
    fun `dispatch - toggles selection flags`() = runTest {
        coEvery { seeder.status() } returns status()

        val vm = LoadInitialCatalogsViewModel(seeder)
        advanceUntilIdle()

        vm.dispatch(LoadInitialCatalogsAction.ToggleWallets(false))
        vm.dispatch(LoadInitialCatalogsAction.ToggleCryptos(false))
        vm.dispatch(LoadInitialCatalogsAction.ToggleFiat(true))
        vm.dispatch(LoadInitialCatalogsAction.ToggleSyncManual(true))

        assertFalse(vm.state.wallets)
        assertFalse(vm.state.cryptos)
        assertTrue(vm.state.fiat)
        assertTrue(vm.state.syncManual)
    }

    @Test
    fun `requestSeed - builds confirm text using effective request (already flags are respected)`() = runTest {
        // wallets ya existen -> effectiveRequest.wallets debe ser false aunque state.wallets sea true
        coEvery { seeder.status() } returns status(wallets = 5, cryptos = 0, fiat = 0)

        val vm = LoadInitialCatalogsViewModel(seeder)
        advanceUntilIdle()

        vm.dispatch(LoadInitialCatalogsAction.RequestSeed)

        assertTrue(vm.state.showConfirm)
        assertTrue(vm.state.confirmText.contains("Se crearán datos predeterminados"))

        // walletsAlready=true => NO debe incluir Portafolio + Carteras
        assertFalse(vm.state.confirmText.contains("Portafolio + Carteras"))

        // cryptos/fiat por default están seleccionados y no existen => sí deben aparecer
        assertTrue(vm.state.confirmText.contains("Catálogo de Cryptos"))
        assertTrue(vm.state.confirmText.contains("Catálogo FIAT"))
    }

    @Test
    fun `cancelSeed - hides confirm and clears confirmText`() = runTest {
        coEvery { seeder.status() } returns status()

        val vm = LoadInitialCatalogsViewModel(seeder)
        advanceUntilIdle()

        vm.dispatch(LoadInitialCatalogsAction.RequestSeed)
        assertTrue(vm.state.showConfirm)

        vm.dispatch(LoadInitialCatalogsAction.CancelSeed)

        assertFalse(vm.state.showConfirm)
        assertEquals("", vm.state.confirmText)
    }

    @Test
    fun `confirmSeed - without pending request does nothing`() = runTest {
        coEvery { seeder.status() } returns status()

        val vm = LoadInitialCatalogsViewModel(seeder)
        advanceUntilIdle()

        val before = vm.state
        vm.dispatch(LoadInitialCatalogsAction.ConfirmSeed)
        advanceUntilIdle()

        assertEquals(before, vm.state)
        coVerify(exactly = 0) { seeder.seed(any()) }
    }

    @Test
    fun `confirmSeed - success calls seeder with effective request, sets lastResult OK and refreshes status`() = runTest {
        val s0 = status(wallets = 0, cryptos = 0, fiat = 0)
        val s1 = status(wallets = 1, cryptos = 10, fiat = 5)

        // init refreshStatus + refreshStatus after seed
        coEvery { seeder.status() } returnsMany listOf(s0, s1)

        val reqSlot = slot<SeedRequest>()
        coEvery { seeder.seed(capture(reqSlot)) } returns SeedResult(
            createdPortfolioId = 123L,
            walletsInserted = 1,
            cryptosUpserted = 10,
            fiatUpserted = 5,
            syncApplied = true
        )

        val vm = LoadInitialCatalogsViewModel(seeder)
        advanceUntilIdle()

        vm.dispatch(LoadInitialCatalogsAction.ToggleSyncManual(true))
        vm.dispatch(LoadInitialCatalogsAction.RequestSeed)
        assertTrue(vm.state.showConfirm)

        vm.dispatch(LoadInitialCatalogsAction.ConfirmSeed)
        advanceUntilIdle()

        // Request efectivo (no “lo que el usuario seleccionó”, sino lo que aplica según already)
        assertTrue(reqSlot.captured.wallets)
        assertTrue(reqSlot.captured.cryptos)
        assertTrue(reqSlot.captured.fiat)
        assertTrue(reqSlot.captured.syncManual)

        assertFalse(vm.state.loading)
        assertNull(vm.state.error)

        val last = vm.state.lastResult
        assertNotNull(last)
        assertTrue(last!!.startsWith("OK:"))
        assertTrue(last.contains("wallets=1"))
        assertTrue(last.contains("cryptos=10"))
        assertTrue(last.contains("fiat=5"))

        // refreshStatus se ejecuta otra vez
        coVerify(exactly = 2) { seeder.status() }
        coVerify(exactly = 1) { seeder.seed(any()) }

        // ya con s1 debe marcar "Already"
        assertTrue(vm.state.walletsAlready)
        assertTrue(vm.state.cryptosAlready)
        assertTrue(vm.state.fiatAlready)

        // confirm debe estar cerrado
        assertFalse(vm.state.showConfirm)
        assertTrue(vm.state.confirmText.isNotBlank())
    }

    @Test
    fun `confirmSeed - failure sets error and lastResult Error and refreshes status`() = runTest {
        val s0 = status()
        val s1 = status() // se vuelve a refrescar aunque falle
        coEvery { seeder.status() } returnsMany listOf(s0, s1)

        coEvery { seeder.seed(any()) } throws IllegalStateException("seed failed")

        val vm = LoadInitialCatalogsViewModel(seeder)
        advanceUntilIdle()

        vm.dispatch(LoadInitialCatalogsAction.RequestSeed)
        vm.dispatch(LoadInitialCatalogsAction.ConfirmSeed)
        advanceUntilIdle()

        assertFalse(vm.state.loading)
        assertNull(vm.state.error)

        val last = vm.state.lastResult
        assertNotNull(last)
        assertTrue(last!!.startsWith("Error:"))

        coVerify(exactly = 2) { seeder.status() }
        coVerify(exactly = 1) { seeder.seed(any()) }
    }

    @Test
    fun `confirmSeed - effective request excludes catalogs that already exist`() = runTest {
        // cryptos ya existen, fiat ya existe, wallets no
        coEvery { seeder.status() } returnsMany listOf(
            status(wallets = 0, cryptos = 7, fiat = 9),
            status(wallets = 1, cryptos = 7, fiat = 9)
        )

        val reqSlot = slot<SeedRequest>()
        coEvery { seeder.seed(capture(reqSlot)) } returns SeedResult(
            createdPortfolioId = 1L,
            walletsInserted = 1,
            cryptosUpserted = 0,
            fiatUpserted = 0,
            syncApplied = false
        )

        val vm = LoadInitialCatalogsViewModel(seeder)
        advanceUntilIdle()

        // aunque estén seleccionados, effectiveRequest debe apagar lo que ya existe
        vm.dispatch(LoadInitialCatalogsAction.RequestSeed)
        vm.dispatch(LoadInitialCatalogsAction.ConfirmSeed)
        advanceUntilIdle()

        assertTrue(reqSlot.captured.wallets)
        assertFalse(reqSlot.captured.cryptos)
        assertFalse(reqSlot.captured.fiat)
    }
}