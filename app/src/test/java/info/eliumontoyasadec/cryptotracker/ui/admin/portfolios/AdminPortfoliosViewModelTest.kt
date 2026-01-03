package info.eliumontoyasadec.cryptotracker.ui.admin.portfolios

import info.eliumontoyasadec.cryptotracker.domain.interactor.portfolio.CreatePortfolioCommand
import info.eliumontoyasadec.cryptotracker.domain.interactor.portfolio.CreatePortfolioResult
import info.eliumontoyasadec.cryptotracker.domain.interactor.portfolio.CreatePortfolioUseCase
import info.eliumontoyasadec.cryptotracker.domain.interactor.portfolio.DeletePortfolioCommand
import info.eliumontoyasadec.cryptotracker.domain.interactor.portfolio.DeletePortfolioResult
import info.eliumontoyasadec.cryptotracker.domain.interactor.portfolio.DeletePortfolioUseCase
import info.eliumontoyasadec.cryptotracker.domain.interactor.portfolio.GetAllPortfoliosUseCase
import info.eliumontoyasadec.cryptotracker.domain.interactor.portfolio.SetDefaultPortfolioCommand
import info.eliumontoyasadec.cryptotracker.domain.interactor.portfolio.SetDefaultPortfolioResult
import info.eliumontoyasadec.cryptotracker.domain.interactor.portfolio.SetDefaultPortfolioUseCase
import info.eliumontoyasadec.cryptotracker.domain.interactor.portfolio.UpdatePortfolioCommand
import info.eliumontoyasadec.cryptotracker.domain.interactor.portfolio.UpdatePortfolioResult
import info.eliumontoyasadec.cryptotracker.domain.interactor.portfolio.UpdatePortfolioUseCase
import info.eliumontoyasadec.cryptotracker.domain.model.Portfolio
import info.eliumontoyasadec.cryptotracker.ui.admin.MainDispatcherRule
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class AdminPortfoliosViewModelTest {


    @get:Rule
    val dispatcher = MainDispatcherRule()

    //private val dispatcher: TestDispatcher = StandardTestDispatcher()

    private val getAll: GetAllPortfoliosUseCase = mockk()
    private val create: CreatePortfolioUseCase = mockk()
    private val update: UpdatePortfolioUseCase = mockk()
    private val delete: DeletePortfolioUseCase = mockk()
    private val setDefault: SetDefaultPortfolioUseCase = mockk()

    private lateinit var vm: AdminPortfoliosViewModel

    @Before
    fun setUp() {
         vm = AdminPortfoliosViewModel(
            getAll = getAll,
            createPortfolio = create,
            updatePortfolio = update,
            deletePortfolio = delete,
            setDefaultPortfolio = setDefault
        )
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `load - success updates items and stops loading`() = runTest {
        val items = listOf(
            Portfolio(portfolioId = 1L, name = "Main", description = "desc", isDefault = true),
            Portfolio(portfolioId = 2L, name = "Alt", description = null, isDefault = false)
        )
        coEvery { getAll.execute() } returns items

        vm.load()
        advanceUntilIdle()

        assertFalse(vm.state.loading)
        assertNull(vm.state.error)
        assertEquals(items, vm.state.items)
        coVerify(exactly = 1) { getAll.execute() }
    }

    @Test
    fun `load - failure sets error and stops loading`() = runTest {
        coEvery { getAll.execute() } throws IllegalStateException("boom")

        vm.load()
        advanceUntilIdle()

        assertFalse(vm.state.loading)
        assertEquals("boom", vm.state.error)
        coVerify(exactly = 1) { getAll.execute() }
    }

    @Test
    fun `openCreate opens form and clears editing`() {
        vm.openCreate()

        assertTrue(vm.state.showForm)
        assertNull(vm.state.editing)
    }

    @Test
    fun `openEdit opens form with item as editing`() {
        val p = Portfolio(portfolioId = 10L, name = "P", description = null, isDefault = false)

        vm.openEdit(p)

        assertTrue(vm.state.showForm)
        assertEquals(p, vm.state.editing)
    }

    @Test
    fun `save when creating calls CreatePortfolioUseCase with correct command and closes form on success`() = runTest {
        val cmdSlot = slot<CreatePortfolioCommand>()
        val updatedItems = listOf(
            Portfolio(portfolioId = 1L, name = "X", description = null, isDefault = false)
        )
        coEvery { create.execute(capture(cmdSlot)) } returns CreatePortfolioResult.Success(updatedItems)

        vm.openCreate()
        vm.save(name = "X", description = null, makeDefault = false)
        advanceUntilIdle()

        assertEquals("X", cmdSlot.captured.nameRaw)
        assertNull(cmdSlot.captured.descriptionRaw)
        assertFalse(cmdSlot.captured.makeDefault)

        assertFalse(vm.state.loading)
        assertNull(vm.state.error)
        assertEquals(updatedItems, vm.state.items)
        assertFalse(vm.state.showForm)
        assertNull(vm.state.editing)

        coVerify(exactly = 1) { create.execute(any()) }
        coVerify(exactly = 0) { update.execute(any()) }
    }

    @Test
    fun `save when editing calls UpdatePortfolioUseCase with correct command and closes form on success`() = runTest {
        val cmdSlot = slot<UpdatePortfolioCommand>()
        val editing = Portfolio(portfolioId = 99L, name = "Old", description = "d", isDefault = false)
        val updatedItems = listOf(
            Portfolio(portfolioId = 99L, name = "New", description = null, isDefault = true)
        )
        coEvery { update.execute(capture(cmdSlot)) } returns UpdatePortfolioResult.Success(updatedItems)

        vm.openEdit(editing)
        vm.save(name = "New", description = null, makeDefault = true)
        advanceUntilIdle()

        assertEquals(99L, cmdSlot.captured.id)
        assertEquals("New", cmdSlot.captured.nameRaw)
        assertNull(cmdSlot.captured.descriptionRaw)
        assertTrue(cmdSlot.captured.makeDefault)

        assertFalse(vm.state.loading)
        assertNull(vm.state.error)
        assertEquals(updatedItems, vm.state.items)
        assertFalse(vm.state.showForm)
        assertNull(vm.state.editing)

        coVerify(exactly = 0) { create.execute(any()) }
        coVerify(exactly = 1) { update.execute(any()) }
    }

    @Test
    fun `create - validation error sets error and keeps form open`() = runTest {
        coEvery { create.execute(any()) } returns CreatePortfolioResult.ValidationError("name required")

        vm.openCreate()
        vm.create(name = "", description = null, makeDefault = false)
        advanceUntilIdle()

        assertFalse(vm.state.loading)
        assertEquals("name required", vm.state.error)
        assertTrue(vm.state.showForm)
        assertNull(vm.state.editing)
    }

    @Test
    fun `delete - success clears pending delete and closes confirm`() = runTest {
        val target = Portfolio(portfolioId = 2L, name = "B", description = null, isDefault = false)
        val after = listOf(
            Portfolio(portfolioId = 1L, name = "A", description = null, isDefault = true)
        )
        coEvery { delete.execute(DeletePortfolioCommand(2L)) } returns DeletePortfolioResult.Success(after)

        vm.requestDelete(target)
        vm.confirmDelete()
        advanceUntilIdle()

        assertFalse(vm.state.loading)
        assertNull(vm.state.error)
        assertEquals(after, vm.state.items)
        assertFalse(vm.state.showDeleteConfirm)
        assertNull(vm.state.pendingDelete)

        coVerify(exactly = 1) { delete.execute(DeletePortfolioCommand(2L)) }
    }

    @Test
    fun `confirmDelete without pendingDelete does nothing`() = runTest {
        val snapshot = vm.state

        vm.confirmDelete()
        advanceUntilIdle()

        assertEquals(snapshot, vm.state)
        coVerify(exactly = 0) { delete.execute(any()) }
    }

    @Test
    fun `setDefault - failure sets error and stops loading`() = runTest {
        coEvery { setDefault.execute(SetDefaultPortfolioCommand(1L)) } returns
                SetDefaultPortfolioResult.Failure("cannot")

        vm.setDefault(1L)
        advanceUntilIdle()

        assertFalse(vm.state.loading)
        assertEquals("cannot", vm.state.error)
        coVerify(exactly = 1) { setDefault.execute(SetDefaultPortfolioCommand(1L)) }
    }
}