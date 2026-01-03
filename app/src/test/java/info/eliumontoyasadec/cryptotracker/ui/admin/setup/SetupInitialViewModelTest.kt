package info.eliumontoyasadec.cryptotracker.ui.admin.setup

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
class SetupInitialViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val deleteAll: suspend () -> Unit = mockk(relaxed = true)
    private val loadCatalogs: suspend () -> Unit = mockk(relaxed = true)
    private val loadMovements: suspend () -> Unit = mockk(relaxed = true)
    private val backupExport: suspend () -> Unit = mockk(relaxed = true)
    private val backupImport: suspend () -> Unit = mockk(relaxed = true)

    private fun newVm(): SetupInitialViewModel =
        SetupInitialViewModel(
            ops = SetupInitialOps(
                deleteAllData = deleteAll,
                loadInitialCatalogs = loadCatalogs,
                loadInitialMovements = loadMovements,
                backupExport = backupExport,
                backupImport = backupImport
            )
        )

    @Test
    fun `initial state`() {
        val vm = newVm()

        assertFalse(vm.state.loading)
        assertNull(vm.state.error)
        assertNull(vm.state.lastActionMessage)
        assertFalse(vm.state.confirmDeleteAll)
    }

    @Test
    fun `RequestDeleteAll - opens confirmation and clears messages`() {
        val vm = newVm()

        vm.dispatch(SetupInitialAction.RequestDeleteAll)

        assertTrue(vm.state.confirmDeleteAll)
        assertNull(vm.state.error)
        assertNull(vm.state.lastActionMessage)
    }

    @Test
    fun `CancelDeleteAll - closes confirmation`() {
        val vm = newVm()

        vm.dispatch(SetupInitialAction.RequestDeleteAll)
        assertTrue(vm.state.confirmDeleteAll)

        vm.dispatch(SetupInitialAction.CancelDeleteAll)

        assertFalse(vm.state.confirmDeleteAll)
    }

    @Test
    fun `ConfirmDeleteAll - runs deleteAllData, closes confirm and finishes without error`() = runTest {
        val vm = newVm()

        vm.dispatch(SetupInitialAction.RequestDeleteAll)
        assertTrue(vm.state.confirmDeleteAll)

        vm.dispatch(SetupInitialAction.ConfirmDeleteAll)

        // se cierra antes de ejecutar el op (before = {...})
        assertFalse(vm.state.confirmDeleteAll)

        advanceUntilIdle()

        assertFalse(vm.state.loading)
        assertNull(vm.state.error)
        assertNull(vm.state.lastActionMessage) // okMsg = null

        coVerify(exactly = 1) { deleteAll.invoke() }
    }

    @Test
    fun `LoadInitialCatalogs - runs loadInitialCatalogs`() = runTest {
        val vm = newVm()

        vm.dispatch(SetupInitialAction.LoadInitialCatalogs)
        advanceUntilIdle()

        assertFalse(vm.state.loading)
        assertNull(vm.state.error)
        coVerify(exactly = 1) { loadCatalogs.invoke() }
    }

    @Test
    fun `LoadInitialMovements - runs loadInitialMovements`() = runTest {
        val vm = newVm()

        vm.dispatch(SetupInitialAction.LoadInitialMovements)
        advanceUntilIdle()

        assertFalse(vm.state.loading)
        assertNull(vm.state.error)
        coVerify(exactly = 1) { loadMovements.invoke() }
    }

    @Test
    fun `BackupExport - runs backupExport`() = runTest {
        val vm = newVm()

        vm.dispatch(SetupInitialAction.BackupExport)
        advanceUntilIdle()

        assertFalse(vm.state.loading)
        assertNull(vm.state.error)
        coVerify(exactly = 1) { backupExport.invoke() }
    }

    @Test
    fun `BackupImport - runs backupImport`() = runTest {
        val vm = newVm()

        vm.dispatch(SetupInitialAction.BackupImport)
        advanceUntilIdle()

        assertFalse(vm.state.loading)
        assertNull(vm.state.error)
        coVerify(exactly = 1) { backupImport.invoke() }
    }

    @Test
    fun `operation failure - sets error and clears loading`() = runTest {
        coEvery { loadCatalogs.invoke() } throws IllegalStateException("boom")

        val vm = newVm()

        vm.dispatch(SetupInitialAction.LoadInitialCatalogs)
        advanceUntilIdle()

        assertFalse(vm.state.loading)
        assertEquals("boom", vm.state.error)
        assertNull(vm.state.lastActionMessage)

        coVerify(exactly = 1) { loadCatalogs.invoke() }
    }

    @Test
    fun `operation failure without message - uses Fallo desconocido`() = runTest {
        coEvery { backupImport.invoke() } throws RuntimeException(null as String?)

        val vm = newVm()

        vm.dispatch(SetupInitialAction.BackupImport)
        advanceUntilIdle()

        assertFalse(vm.state.loading)
        assertEquals("Fallo desconocido", vm.state.error)

        coVerify(exactly = 1) { backupImport.invoke() }
    }
}