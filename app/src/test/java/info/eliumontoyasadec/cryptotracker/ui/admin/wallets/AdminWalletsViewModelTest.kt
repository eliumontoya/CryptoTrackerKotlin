package info.eliumontoyasadec.cryptotracker.ui.admin.wallets

import info.eliumontoyasadec.cryptotracker.domain.interactor.wallet.DeleteWalletCommand
import info.eliumontoyasadec.cryptotracker.domain.interactor.wallet.DeleteWalletResult
import info.eliumontoyasadec.cryptotracker.domain.interactor.wallet.DeleteWalletUseCase
import info.eliumontoyasadec.cryptotracker.domain.interactor.wallet.GetWalletsByPortfolioCommand
import info.eliumontoyasadec.cryptotracker.domain.interactor.wallet.GetWalletsByPortfolioUseCase
import info.eliumontoyasadec.cryptotracker.domain.interactor.wallet.LoadAdminWalletsContextResult
import info.eliumontoyasadec.cryptotracker.domain.interactor.wallet.LoadAdminWalletsContextUseCase
import info.eliumontoyasadec.cryptotracker.domain.interactor.wallet.SetMainWalletCommand
import info.eliumontoyasadec.cryptotracker.domain.interactor.wallet.SetMainWalletResult
import info.eliumontoyasadec.cryptotracker.domain.interactor.wallet.SetMainWalletUseCase
import info.eliumontoyasadec.cryptotracker.domain.interactor.wallet.UpsertWalletCommand
import info.eliumontoyasadec.cryptotracker.domain.interactor.wallet.UpsertWalletResult
import info.eliumontoyasadec.cryptotracker.domain.interactor.wallet.UpsertWalletUseCase
import info.eliumontoyasadec.cryptotracker.domain.model.Portfolio
import info.eliumontoyasadec.cryptotracker.domain.model.Wallet
import info.eliumontoyasadec.cryptotracker.ui.admin.MainDispatcherRule
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class AdminWalletsViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val loadContext: LoadAdminWalletsContextUseCase = mockk()
    private val getWalletsByPortfolio: GetWalletsByPortfolioUseCase = mockk()
    private val upsertWallet: UpsertWalletUseCase = mockk()
    private val deleteWallet: DeleteWalletUseCase = mockk()
    private val setMainWallet: SetMainWalletUseCase = mockk()

    private fun newVm() = AdminWalletsViewModel(
        loadContext = loadContext,
        getWalletsByPortfolio = getWalletsByPortfolio,
        upsertWallet = upsertWallet,
        deleteWallet = deleteWallet,
        setMainWallet = setMainWallet
    )

    @Test
    fun `start - success loads portfolios, selectedPortfolioId and wallets`() = runTest {
        val portfolios = listOf(
            Portfolio(portfolioId = 1L, name = "P1", description = null, isDefault = true),
            Portfolio(portfolioId = 2L, name = "P2", description = "x", isDefault = false)
        )
        val wallets = listOf(
            Wallet(walletId = 10L, portfolioId = 1L, name = "W1", isMain = true),
            Wallet(walletId = 11L, portfolioId = 1L, name = "W2", isMain = false)
        )
        coEvery { loadContext.execute() } returns LoadAdminWalletsContextResult.Success(
            portfolios = portfolios,
            selectedPortfolioId = 1L,
            wallets = wallets
        )

        val vm = newVm()

        vm.start()
        advanceUntilIdle()

        assertFalse(vm.state.loading)
        assertNull(vm.state.error)
        assertEquals(portfolios, vm.state.portfolios)
        assertEquals(1L, vm.state.selectedPortfolioId)
        assertEquals(wallets, vm.state.items)

        coVerify(exactly = 1) { loadContext.execute() }
    }

    @Test
    fun `start - failure sets error and stops loading`() = runTest {
        coEvery { loadContext.execute() } returns LoadAdminWalletsContextResult.Failure("boom")

        val vm = newVm()

        vm.start()
        advanceUntilIdle()

        assertFalse(vm.state.loading)
        assertEquals("boom", vm.state.error)

        coVerify(exactly = 1) { loadContext.execute() }
    }

    @Test
    fun `selectPortfolio - if same id does nothing`() = runTest {
        val portfolios = listOf(
            Portfolio(portfolioId = 1L, name = "P1", description = null, isDefault = true)
        )
        val wallets = listOf(
            Wallet(walletId = 10L, portfolioId = 1L, name = "W1", isMain = true)
        )

        coEvery { loadContext.execute() } returns LoadAdminWalletsContextResult.Success(
            portfolios = portfolios,
            selectedPortfolioId = 1L,
            wallets = wallets
        )

        val vm = newVm()

        vm.start()
        advanceUntilIdle()

        // mismo id -> debe regresar sin hacer nada
        vm.selectPortfolio(1L)
        advanceUntilIdle()

        assertFalse(vm.state.loading)
        assertNull(vm.state.error)
        assertEquals(1L, vm.state.selectedPortfolioId)
        assertEquals(wallets, vm.state.items)

        // clave: no debe consultar wallets de nuevo
        coVerify(exactly = 0) { getWalletsByPortfolio.execute(any()) }
    }

    @Test
    fun `selectPortfolio - loads wallets for new portfolio id`() = runTest {
        val portfolios = listOf(
            Portfolio(portfolioId = 1L, name = "P1", description = null, isDefault = true),
            Portfolio(portfolioId = 2L, name = "P2", description = null, isDefault = false)
        )
        val initialWallets = listOf(
            Wallet(walletId = 10L, portfolioId = 1L, name = "W1", isMain = true)
        )
        coEvery { loadContext.execute() } returns LoadAdminWalletsContextResult.Success(
            portfolios = portfolios,
            selectedPortfolioId = 1L,
            wallets = initialWallets
        )

        val newWallets = listOf(
            Wallet(walletId = 20L, portfolioId = 2L, name = "W-A", isMain = true),
            Wallet(walletId = 21L, portfolioId = 2L, name = "W-B", isMain = false)
        )
        coEvery { getWalletsByPortfolio.execute(GetWalletsByPortfolioCommand(2L)) } returns newWallets

        val vm = newVm()

        vm.start()
        advanceUntilIdle()

        vm.selectPortfolio(2L)
        advanceUntilIdle()

        assertFalse(vm.state.loading)
        assertNull(vm.state.error)
        assertEquals(2L, vm.state.selectedPortfolioId)
        assertEquals(newWallets, vm.state.items)

        coVerify(exactly = 1) { getWalletsByPortfolio.execute(GetWalletsByPortfolioCommand(2L)) }
    }

    @Test
    fun `selectPortfolio - failure sets error and stops loading`() = runTest {
        val portfolios = listOf(
            Portfolio(portfolioId = 1L, name = "P1", description = null, isDefault = true),
            Portfolio(portfolioId = 2L, name = "P2", description = null, isDefault = false)
        )
        coEvery { loadContext.execute() } returns LoadAdminWalletsContextResult.Success(
            portfolios = portfolios,
            selectedPortfolioId = 1L,
            wallets = emptyList()
        )
        coEvery { getWalletsByPortfolio.execute(GetWalletsByPortfolioCommand(2L)) } throws
                IllegalStateException("fail")

        val vm = newVm()

        vm.start()
        advanceUntilIdle()

        vm.selectPortfolio(2L)
        advanceUntilIdle()

        assertFalse(vm.state.loading)
        assertEquals("fail", vm.state.error)
        assertEquals(2L, vm.state.selectedPortfolioId)

        coVerify(exactly = 1) { getWalletsByPortfolio.execute(GetWalletsByPortfolioCommand(2L)) }
    }

    @Test
    fun `openCreate opens editor with empty fields`() = runTest {
        val vm = newVm()

        vm.openCreate()

        assertTrue(vm.state.showEditor)
        assertNull(vm.state.editorWalletId)
        assertEquals("", vm.state.editorName)
        assertFalse(vm.state.editorMakeMain)
    }

    @Test
    fun `openEdit opens editor with wallet data`() = runTest {
        val vm = newVm()
        val w = Wallet(walletId = 7L, portfolioId = 1L, name = "Caja Fuerte", isMain = true)

        vm.openEdit(w)

        assertTrue(vm.state.showEditor)
        assertEquals(7L, vm.state.editorWalletId)
        assertEquals("Caja Fuerte", vm.state.editorName)
        assertTrue(vm.state.editorMakeMain)
    }

    @Test
    fun `closeEditor hides editor`() = runTest {
        val vm = newVm()

        vm.openCreate()
        assertTrue(vm.state.showEditor)

        vm.closeEditor()
        assertFalse(vm.state.showEditor)
    }

    @Test
    fun `saveEditor - success updates items and closes editor`() = runTest {
        val portfolios = listOf(
            Portfolio(portfolioId = 1L, name = "P1", description = null, isDefault = true)
        )
        coEvery { loadContext.execute() } returns LoadAdminWalletsContextResult.Success(
            portfolios = portfolios,
            selectedPortfolioId = 1L,
            wallets = emptyList()
        )

        val cmdSlot = slot<UpsertWalletCommand>()
        val updated = listOf(
            Wallet(walletId = 100L, portfolioId = 1L, name = "Nueva", isMain = false)
        )
        coEvery { upsertWallet.execute(capture(cmdSlot)) } returns UpsertWalletResult.Success(updated)

        val vm = newVm()
        vm.start()
        advanceUntilIdle()

        vm.openCreate()
        vm.onEditorNameChange("Nueva")
        vm.onEditorMakeMainChange(false)

        vm.saveEditor()
        advanceUntilIdle()

        assertEquals(1L, cmdSlot.captured.portfolioId)
        assertNull(cmdSlot.captured.walletId)
        assertEquals("Nueva", cmdSlot.captured.nameRaw)
        assertFalse(cmdSlot.captured.makeMain)

        assertFalse(vm.state.loading)
        assertNull(vm.state.error)
        assertEquals(updated, vm.state.items)
        assertFalse(vm.state.showEditor)

        coVerify(exactly = 1) { upsertWallet.execute(any()) }
    }

    @Test
    fun `saveEditor - validation error keeps editor open and sets error`() = runTest {
        val portfolios = listOf(
            Portfolio(portfolioId = 1L, name = "P1", description = null, isDefault = true)
        )
        coEvery { loadContext.execute() } returns LoadAdminWalletsContextResult.Success(
            portfolios = portfolios,
            selectedPortfolioId = 1L,
            wallets = emptyList()
        )
        coEvery { upsertWallet.execute(any()) } returns UpsertWalletResult.ValidationError("name required")

        val vm = newVm()
        vm.start()
        advanceUntilIdle()

        vm.openCreate()
        vm.onEditorNameChange("")
        vm.saveEditor()
        advanceUntilIdle()

        assertFalse(vm.state.loading)
        assertEquals("name required", vm.state.error)
        assertTrue(vm.state.showEditor)

        coVerify(exactly = 1) { upsertWallet.execute(any()) }
    }

    @Test
    fun `saveEditor - failure sets error and keeps editor open`() = runTest {
        val portfolios = listOf(
            Portfolio(portfolioId = 1L, name = "P1", description = null, isDefault = true)
        )
        coEvery { loadContext.execute() } returns LoadAdminWalletsContextResult.Success(
            portfolios = portfolios,
            selectedPortfolioId = 1L,
            wallets = emptyList()
        )
        coEvery { upsertWallet.execute(any()) } returns UpsertWalletResult.Failure("nope")

        val vm = newVm()
        vm.start()
        advanceUntilIdle()

        vm.openCreate()
        vm.onEditorNameChange("X")
        vm.saveEditor()
        advanceUntilIdle()

        assertFalse(vm.state.loading)
        assertEquals("nope", vm.state.error)
        assertTrue(vm.state.showEditor)

        coVerify(exactly = 1) { upsertWallet.execute(any()) }
    }

    @Test
    fun `deleteWallet - success updates items`() = runTest {
        val portfolios = listOf(
            Portfolio(portfolioId = 1L, name = "P1", description = null, isDefault = true)
        )
        val initial = listOf(
            Wallet(walletId = 10L, portfolioId = 1L, name = "W1", isMain = true),
            Wallet(walletId = 11L, portfolioId = 1L, name = "W2", isMain = false)
        )
        coEvery { loadContext.execute() } returns LoadAdminWalletsContextResult.Success(
            portfolios = portfolios,
            selectedPortfolioId = 1L,
            wallets = initial
        )

        val after = listOf(
            Wallet(walletId = 10L, portfolioId = 1L, name = "W1", isMain = true)
        )
        coEvery { deleteWallet.execute(DeleteWalletCommand(1L, 11L)) } returns DeleteWalletResult.Success(after)

        val vm = newVm()
        vm.start()
        advanceUntilIdle()

        vm.deleteWallet(11L)
        advanceUntilIdle()

        assertFalse(vm.state.loading)
        assertNull(vm.state.error)
        assertEquals(after, vm.state.items)

        coVerify(exactly = 1) { deleteWallet.execute(DeleteWalletCommand(1L, 11L)) }
    }

    @Test
    fun `deleteWallet - failure sets error and keeps items from result`() = runTest {
        val portfolios = listOf(
            Portfolio(portfolioId = 1L, name = "P1", description = null, isDefault = true)
        )
        val initial = listOf(
            Wallet(walletId = 10L, portfolioId = 1L, name = "W1", isMain = true),
            Wallet(walletId = 11L, portfolioId = 1L, name = "W2", isMain = false)
        )
        coEvery { loadContext.execute() } returns LoadAdminWalletsContextResult.Success(
            portfolios = portfolios,
            selectedPortfolioId = 1L,
            wallets = initial
        )

        coEvery { deleteWallet.execute(DeleteWalletCommand(1L, 11L)) } returns
                DeleteWalletResult.Failure(message = "cannot", items = initial)

        val vm = newVm()
        vm.start()
        advanceUntilIdle()

        vm.deleteWallet(11L)
        advanceUntilIdle()

        assertFalse(vm.state.loading)
        assertEquals("cannot", vm.state.error)
        assertEquals(initial, vm.state.items)

        coVerify(exactly = 1) { deleteWallet.execute(DeleteWalletCommand(1L, 11L)) }
    }

    @Test
    fun `makeMain - success updates items`() = runTest {
        val portfolios = listOf(
            Portfolio(portfolioId = 1L, name = "P1", description = null, isDefault = true)
        )
        coEvery { loadContext.execute() } returns LoadAdminWalletsContextResult.Success(
            portfolios = portfolios,
            selectedPortfolioId = 1L,
            wallets = emptyList()
        )

        val after = listOf(
            Wallet(walletId = 10L, portfolioId = 1L, name = "W1", isMain = true)
        )
        coEvery { setMainWallet.execute(SetMainWalletCommand(1L, 10L)) } returns
                SetMainWalletResult.Success(after)

        val vm = newVm()
        vm.start()
        advanceUntilIdle()

        vm.makeMain(10L)
        advanceUntilIdle()

        assertFalse(vm.state.loading)
        assertNull(vm.state.error)
        assertEquals(after, vm.state.items)

        coVerify(exactly = 1) { setMainWallet.execute(SetMainWalletCommand(1L, 10L)) }
    }

    @Test
    fun `makeMain - failure sets error`() = runTest {
        val portfolios = listOf(
            Portfolio(portfolioId = 1L, name = "P1", description = null, isDefault = true)
        )
        coEvery { loadContext.execute() } returns LoadAdminWalletsContextResult.Success(
            portfolios = portfolios,
            selectedPortfolioId = 1L,
            wallets = emptyList()
        )
        coEvery { setMainWallet.execute(SetMainWalletCommand(1L, 10L)) } returns
                SetMainWalletResult.Failure("nope")

        val vm = newVm()
        vm.start()
        advanceUntilIdle()

        vm.makeMain(10L)
        advanceUntilIdle()

        assertFalse(vm.state.loading)
        assertEquals("nope", vm.state.error)

        coVerify(exactly = 1) { setMainWallet.execute(SetMainWalletCommand(1L, 10L)) }
    }
}