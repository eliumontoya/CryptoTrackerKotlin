package info.eliumontoyasadec.cryptotracker.ui.admin.fiat

import info.eliumontoyasadec.cryptotracker.domain.model.Fiat
import info.eliumontoyasadec.cryptotracker.domain.interactor.fiat.*
import info.eliumontoyasadec.cryptotracker.ui.admin.MainDispatcherRule
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import kotlinx.coroutines.test.runTest
import java.io.IOException

@OptIn(ExperimentalCoroutinesApi::class)
class AdminFiatViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val getAllFiats: GetAllFiatsUseCase = mockk()
    private val upsertFiat: UpsertFiatUseCase = mockk()
    private val deleteFiat: DeleteFiatUseCase = mockk()

    private fun createVm(): AdminFiatViewModel =
        AdminFiatViewModel(
            getAllFiats = getAllFiats,
            upsertFiat = upsertFiat,
            deleteFiat = deleteFiat
        )

    @Test
    fun `load success - sets items and toggles loading`() = runTest {
        val items = listOf(
            Fiat("USD", "US Dollar", "USD"),
            Fiat("MXN", "Peso Mexicano", "MXN")
        )
        coEvery { getAllFiats.execute() } returns items

        val vm = createVm()
        assertFalse(vm.state.loading)

        vm.load()
       advanceUntilIdle()

        assertFalse(vm.state.loading)
        assertNull(vm.state.error)
        assertEquals(items, vm.state.items)

        coVerify(exactly = 1) { getAllFiats.execute() }
    }

    @Test
    fun `load failure - sets error and toggles loading`() = runTest {
        coEvery { getAllFiats.execute() } throws IOException("boom")

        val vm = createVm()

        vm.load()
        advanceUntilIdle()

        assertFalse(vm.state.loading)
        assertEquals("boom", vm.state.error)
        assertEquals(emptyList<Fiat>(), vm.state.items)

        coVerify(exactly = 1) { getAllFiats.execute() }
    }

    @Test
    fun `openCreate - shows form with no editing`() = runTest {
        val vm = createVm()

        vm.openCreate()

        assertTrue(vm.state.showForm)
        assertNull(vm.state.editing)
        assertNull(vm.state.error)
    }

    @Test
    fun `openEdit - shows form with editing item`() = runTest {
        val vm = createVm()
        val item = Fiat("USD", "US Dollar", "USD")

        vm.openEdit(item)

        assertTrue(vm.state.showForm)
        assertEquals(item, vm.state.editing)
        assertNull(vm.state.error)
    }

    @Test
    fun `dismissForm - closes form and clears editing`() = runTest {
        val vm = createVm()

        vm.openEdit(Fiat(code = "USD", name = "US Dollar", symbol = "USD"))

        vm.dismissForm()

        assertFalse(vm.state.showForm)
        assertNull(vm.state.editing)
    }

    @Test
    fun `save success - updates items, closes form, sets success message`() = runTest {
        val cmdSlot = slot<UpsertFiatCommand>()
        val updatedItems = listOf(
            Fiat("USD", "US Dollar", "USD"),
            Fiat("EUR", "Euro", "EUR")
        )

        coEvery { upsertFiat.execute(capture(cmdSlot)) } returns
                UpsertFiatResult.Success(items = updatedItems, wasUpdate = false)
        val vm = createVm()
        vm.openCreate()

        vm.save(code = "EUR", name = "Euro", symbol = "EUR")
     advanceUntilIdle()

        assertFalse(vm.state.loading)
        assertNull(vm.state.error)
        assertEquals(updatedItems, vm.state.items)
        assertFalse(vm.state.showForm)
        assertNull(vm.state.editing)

        assertEquals("EUR", cmdSlot.captured.codeRaw)
        assertEquals("Euro", cmdSlot.captured.nameRaw)
        assertEquals("EUR", cmdSlot.captured.symbolRaw)

        coVerify(exactly = 1) { upsertFiat.execute(any()) }
    }

    @Test
    fun `save validation error - keeps form open and sets error`() = runTest {
        coEvery { upsertFiat.execute(any()) } returns UpsertFiatResult.ValidationError("Código inválido")

        val vm = createVm()
        vm.openCreate()

        vm.save(code = "", name = "Euro", symbol = "EUR" )
         advanceUntilIdle()

        assertFalse(vm.state.loading)
        assertEquals("Código inválido", vm.state.error)
        assertTrue(vm.state.showForm)

        coVerify(exactly = 1) { upsertFiat.execute(any()) }
    }

    @Test
    fun `save failure - keeps form open and sets error`() = runTest {
        coEvery { upsertFiat.execute(any()) } returns UpsertFiatResult.Failure("Fallo al guardar")

        val vm = createVm()
        vm.openCreate()

        vm.save(code = "EUR", name = "Euro", symbol = "EUR" )
       advanceUntilIdle()

        assertFalse(vm.state.loading)
        assertEquals("Fallo al guardar", vm.state.error)
        assertTrue(vm.state.showForm)

        coVerify(exactly = 1) { upsertFiat.execute(any()) }
    }

    @Test
    fun `requestDelete - sets pendingDelete and shows confirm`() = runTest {
        val vm = createVm()
        val items = listOf(
            Fiat("USD", "US Dollar", "USD"),
            Fiat("MXN", "Peso Mexicano", "MXN")
        )
        vm.state = vm.state.copy(items = items)

        vm.requestDelete(items[1])

        assertEquals(items[1], vm.state.pendingDelete)
        assertTrue(vm.state.showDeleteConfirm)
        assertNull(vm.state.error)
    }

    @Test
    fun `cancelDelete - clears pendingDelete and closes confirm`() = runTest {
        val vm = createVm()
        vm.state = vm.state.copy(
            pendingDelete = Fiat("MXN", "Peso Mexicano", "MXN"),
            showDeleteConfirm = true
        )

        vm.cancelDelete()

        assertNull(vm.state.pendingDelete)
        assertFalse(vm.state.showDeleteConfirm)
    }

    @Test
    fun `confirmDelete deleted - clears pending, updates items, sets message`() = runTest {
        val cmdSlot = slot<DeleteFiatCommand>()
        val remaining = listOf(Fiat("USD", "US Dollar", "USD"))

        coEvery { deleteFiat.execute(capture(cmdSlot)) } returns DeleteFiatResult.Deleted(remaining)

        val vm = createVm()
        vm.state = vm.state.copy(
            items = listOf(
                Fiat("USD", "US Dollar", "USD"),
                Fiat("MXN", "Peso Mexicano", "MXN")
            ),
            pendingDelete = Fiat("MXN", "Peso Mexicano", "MXN"),
            showDeleteConfirm = true
        )

        vm.confirmDelete()
        advanceUntilIdle()

        assertFalse(vm.state.loading)
        assertNull(vm.state.error)
        assertEquals(remaining, vm.state.items)
        assertNull(vm.state.pendingDelete)
        assertFalse(vm.state.showDeleteConfirm)

        assertEquals("MXN", cmdSlot.captured.codeRaw)

        coVerify(exactly = 1) { deleteFiat.execute(any()) }
    }

    @Test
    fun `confirmDelete not found - sets error and keeps items from result`() = runTest {
        val fromResult = listOf(Fiat("USD", "US Dollar", "USD"))

        coEvery { deleteFiat.execute(any()) } returns DeleteFiatResult.NotFound(
            items = fromResult
        )

        val vm = createVm()
        vm.state = vm.state.copy(
            pendingDelete = Fiat("MXN", "Peso Mexicano", "MXN"),
            showDeleteConfirm = true
        )

        vm.confirmDelete()
     advanceUntilIdle()

        assertFalse(vm.state.loading)
        assertNull(vm.state.error)
        assertEquals(fromResult, vm.state.items)
        assertNull(vm.state.pendingDelete)
        assertFalse(vm.state.showDeleteConfirm)

        coVerify(exactly = 1) { deleteFiat.execute(any()) }
    }

    @Test
    fun `confirmDelete failure - sets error and keeps items from result`() = runTest {
        val fromResult = listOf(Fiat("USD", "US Dollar", "USD"))

        coEvery { deleteFiat.execute(any()) } returns DeleteFiatResult.Failure(
            items = fromResult,
            message = "Error al eliminar"
        )

        val vm = createVm()
        vm.state = vm.state.copy(
            pendingDelete = Fiat("MXN", "Peso Mexicano", "MXN"),
            showDeleteConfirm = true
        )

        vm.confirmDelete()
       advanceUntilIdle()

        assertFalse(vm.state.loading)
        assertEquals("Error al eliminar", vm.state.error)
        assertEquals(fromResult, vm.state.items)
        assertNull(vm.state.pendingDelete)
        assertFalse(vm.state.showDeleteConfirm)

        coVerify(exactly = 1) { deleteFiat.execute(any()) }
    }
}