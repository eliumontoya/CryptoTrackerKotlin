package info.eliumontoyasadec.cryptotracker.ui.admin.delete

import info.eliumontoyasadec.cryptotracker.data.seed.DatabaseWiper
import info.eliumontoyasadec.cryptotracker.data.seed.DeleteRequest
import info.eliumontoyasadec.cryptotracker.data.seed.DeleteResult
import info.eliumontoyasadec.cryptotracker.ui.admin.MainDispatcherRule
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class DeleteDataViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val wiper: DatabaseWiper = mockk()

    private fun newVm() = DeleteDataViewModel(wiper)

    private fun result(
        deletedAll: Boolean = false,
        deletedWallets: Boolean = false,
        deletedCryptos: Boolean = false,
        deletedFiat: Boolean = false,
        deletedMovements: Boolean = false,
        deletedHoldings: Boolean = false,
        deletedPortfolio: Boolean = false,
        walletsCount: Int = 0,
        cryptosCount: Int = 0,
        fiatCount: Int = 0,
        movementsCount: Int = 0,
        holdingsCount: Int = 0,
        portfoliosCount: Int = 0
    ) = DeleteResult(
        deletedWallets = deletedWallets,
        deletedCryptos = deletedCryptos,
        deletedFiat = deletedFiat,
        deletedMovements = deletedMovements,
        deletedHoldings = deletedHoldings,
        deletedPortfolio = deletedPortfolio,
        deletedAllTables = deletedAll,
        walletsDeletedCount = walletsCount,
        cryptosDeletedCount = cryptosCount,
        fiatDeletedCount = fiatCount,
        movementsDeletedCount = movementsCount,
        holdingsDeletedCount = holdingsCount,
        portfoliosDeletedCount = portfoliosCount
    )

    @Test
    fun `initial state - defaults have anySelected true`() {
        val vm = newVm()

        assertFalse(vm.state.all)
        // defaults en UiState: cryptos/wallets/fiat/movements/holdings/portfolio = true
        assertTrue(vm.state.anySelected)
        assertFalse(vm.state.loading)
        assertFalse(vm.state.showConfirm)
        assertFalse(vm.state.showResult)
        assertNull(vm.state.pendingRequest)
        assertNull(vm.state.lastResult)
        assertNull(vm.state.lastError)
    }

    @Test
    fun `toggle all on - sets all flags to true`() {
        val vm = newVm()

        vm.dispatch(DeleteDataAction.ToggleAll(true))

        assertTrue(vm.state.all)
        assertTrue(vm.state.wallets)
        assertTrue(vm.state.cryptos)
        assertTrue(vm.state.fiat)
        assertTrue(vm.state.movements)
        assertTrue(vm.state.holdings)
        assertTrue(vm.state.portfolio)
        assertTrue(vm.state.anySelected)
    }

    @Test
    fun `toggle all off - only clears all flag, keeps others unchanged`() {
        val vm = newVm()

        vm.dispatch(DeleteDataAction.ToggleAll(true))
        vm.dispatch(DeleteDataAction.ToggleCryptos(false))
        vm.dispatch(DeleteDataAction.ToggleAll(false))

        assertFalse(vm.state.all)
        // los toggles individuales permanecen (así está implementado)
        assertFalse(vm.state.cryptos)
        assertTrue(vm.state.wallets)
    }

    @Test
    fun `request delete - opens confirm and sets pending request`() {
        val vm = newVm()

        // ajusta algunas banderas
        vm.dispatch(DeleteDataAction.ToggleCryptos(false))
        vm.dispatch(DeleteDataAction.ToggleFiat(true))

        vm.dispatch(DeleteDataAction.RequestDelete)

        assertTrue(vm.state.showConfirm)
        assertNotNull(vm.state.pendingRequest)

        val req = vm.state.pendingRequest!!
        assertFalse(req.all)
        assertTrue(req.wallets)
        assertFalse(req.cryptos)
        assertTrue(req.fiat)
        assertTrue(req.movements)
        assertTrue(req.holdings)
        assertTrue(req.portfolio)
    }

    @Test
    fun `cancel confirm - closes confirm and clears pendingRequest`() {
        val vm = newVm()

        vm.dispatch(DeleteDataAction.RequestDelete)
        assertTrue(vm.state.showConfirm)
        assertNotNull(vm.state.pendingRequest)

        vm.dispatch(DeleteDataAction.CancelConfirm)

        assertFalse(vm.state.showConfirm)
        assertNull(vm.state.pendingRequest)
    }

    @Test
    fun `confirm delete - without pendingRequest does nothing`() = runTest {
        val vm = newVm()
        val before = vm.state

        vm.dispatch(DeleteDataAction.ConfirmDelete)
        advanceUntilIdle()

        assertEquals(before, vm.state)
        coVerify(exactly = 0) { wiper.wipe(any()) }
    }

    @Test
    fun `confirm delete - success calls wiper and shows result`() = runTest {
        val vm = newVm()

        vm.dispatch(DeleteDataAction.ToggleAll(true))
        vm.dispatch(DeleteDataAction.RequestDelete)

        val expectedReq = DeleteRequest(
            all = true,
            wallets = true,
            cryptos = true,
            fiat = true,
            movements = true,
            holdings = true,
            portfolio = true
        )

        val expectedRes = result(
            deletedAll = true,
            deletedWallets = true,
            deletedCryptos = true,
            deletedFiat = true,
            deletedMovements = true,
            deletedHoldings = true,
            deletedPortfolio = true,
            walletsCount = 2,
            cryptosCount = 10,
            fiatCount = 3,
            movementsCount = 8,
            holdingsCount = 5,
            portfoliosCount = 1
        )

        coEvery { wiper.wipe(expectedReq) } returns expectedRes

        vm.dispatch(DeleteDataAction.ConfirmDelete)
        advanceUntilIdle()

        assertFalse(vm.state.loading)
        assertFalse(vm.state.showConfirm)
        assertTrue(vm.state.showResult)

        assertEquals(expectedRes, vm.state.lastResult)
        assertNull(vm.state.lastError)

        // siempre se limpia al final
        assertNull(vm.state.pendingRequest)

        coVerify(exactly = 1) { wiper.wipe(expectedReq) }
    }

    @Test
    fun `confirm delete - failure sets lastError and shows result`() = runTest {
        val vm = newVm()

        vm.dispatch(DeleteDataAction.ToggleCryptos(false))
        vm.dispatch(DeleteDataAction.ToggleFiat(false))
        vm.dispatch(DeleteDataAction.RequestDelete)

        val expectedReq = DeleteRequest(
            all = false,
            wallets = true,
            cryptos = false,
            fiat = false,
            movements = true,
            holdings = true,
            portfolio = true
        )

        coEvery { wiper.wipe(expectedReq) } throws IllegalStateException("wipe failed")

        vm.dispatch(DeleteDataAction.ConfirmDelete)
        advanceUntilIdle()

        assertFalse(vm.state.loading)
        assertTrue(vm.state.showResult)
        assertNull(vm.state.lastResult)
        assertEquals("wipe failed", vm.state.lastError)
        assertNull(vm.state.pendingRequest)

        coVerify(exactly = 1) { wiper.wipe(expectedReq) }
    }

    @Test
    fun `dismiss result - clears result and error`() = runTest {
        val vm = newVm()

        vm.dispatch(DeleteDataAction.ToggleAll(true))
        vm.dispatch(DeleteDataAction.RequestDelete)

        val expectedReq = DeleteRequest(
            all = true,
            wallets = true,
            cryptos = true,
            fiat = true,
            movements = true,
            holdings = true,
            portfolio = true
        )

        coEvery { wiper.wipe(expectedReq) } returns result(deletedAll = true)

        vm.dispatch(DeleteDataAction.ConfirmDelete)
        advanceUntilIdle()

        assertTrue(vm.state.showResult)
        assertNotNull(vm.state.lastResult)

        vm.dispatch(DeleteDataAction.DismissResult)

        assertFalse(vm.state.showResult)
        assertNull(vm.state.lastResult)
        assertNull(vm.state.lastError)
    }
}