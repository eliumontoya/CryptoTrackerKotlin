package info.eliumontoyasadec.cryptotracker.ui.admin.cryptos

 import info.eliumontoyasadec.cryptotracker.domain.interactor.crypto.DeleteCryptoCommand
import info.eliumontoyasadec.cryptotracker.domain.interactor.crypto.DeleteCryptoResult
import info.eliumontoyasadec.cryptotracker.domain.interactor.crypto.DeleteCryptoUseCase
import info.eliumontoyasadec.cryptotracker.domain.interactor.crypto.GetAllCryptosUseCase
import info.eliumontoyasadec.cryptotracker.domain.interactor.crypto.UpsertCryptoCommand
import info.eliumontoyasadec.cryptotracker.domain.interactor.crypto.UpsertCryptoResult
import info.eliumontoyasadec.cryptotracker.domain.interactor.crypto.UpsertCryptoUseCase
import info.eliumontoyasadec.cryptotracker.domain.model.Crypto
 import info.eliumontoyasadec.cryptotracker.ui.admin.MainDispatcherRule
 import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Before
import org.junit.Rule
import org.junit.Test

class AdminCryptosViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val getAllCryptos: GetAllCryptosUseCase = mockk()
    private val upsertCrypto: UpsertCryptoUseCase = mockk()
    private val deleteCrypto: DeleteCryptoUseCase = mockk()

    private lateinit var vm: AdminCryptosViewModel

    @Before
    fun setUp() {
        vm = AdminCryptosViewModel(
            getAllCryptos = getAllCryptos,
            upsertCrypto = upsertCrypto,
            deleteCrypto = deleteCrypto
        )
    }

    // -------------------------
    // load()
    // -------------------------

    @Test
    fun `load success - sets items, clears error, toggles loading`() = runTest {
        val expected = listOf(
            Crypto(symbol = "BTC", name = "Bitcoin", isActive = true),
            Crypto(symbol = "ETH", name = "Ethereum", isActive = true)
        )
        coEvery { getAllCryptos.execute() } returns expected

        vm.load()
        advanceUntilIdle()

        assertFalse(vm.state.loading)
        assertNull(vm.state.error)
        assertNull(vm.state.lastActionMessage)
        assertEquals(expected, vm.state.items)

        coVerify(exactly = 1) { getAllCryptos.execute() }
    }

    @Test
    fun `load failure - sets error message and toggles loading`() = runTest {
        coEvery { getAllCryptos.execute() } throws RuntimeException("boom")

        vm.load()
        advanceUntilIdle()

        assertFalse(vm.state.loading)
        assertEquals("boom", vm.state.error)
        assertNull(vm.state.lastActionMessage)

        coVerify(exactly = 1) { getAllCryptos.execute() }
    }

    // -------------------------
    // openCreate / openEdit / drafts
    // -------------------------

    @Test
    fun `openCreate - opens form with defaults`() {
        vm.openCreate()

        assertTrue(vm.state.showForm)
        assertFalse(vm.state.isEditing)
        assertEquals("", vm.state.draftSymbol)
        assertEquals("", vm.state.draftName)
        assertTrue(vm.state.draftActive)
        assertNull(vm.state.error)
        assertNull(vm.state.lastActionMessage)
    }

    @Test
    fun `openEdit - opens form with item values and sets editing`() {
        val item = Crypto(symbol = "BTC", name = "Bitcoin", isActive = false)

        vm.openEdit(item)

        assertTrue(vm.state.showForm)
        assertTrue(vm.state.isEditing)
        assertEquals("BTC", vm.state.draftSymbol)
        assertEquals("Bitcoin", vm.state.draftName)
        assertFalse(vm.state.draftActive)
        assertNull(vm.state.error)
        assertNull(vm.state.lastActionMessage)
    }

    @Test
    fun `onDraftSymbolChange - ignored while editing`() {
        val item = Crypto(symbol = "BTC", name = "Bitcoin", isActive = true)
        vm.openEdit(item)

        vm.onDraftSymbolChange("ETH")

        assertEquals("BTC", vm.state.draftSymbol) // no cambia
    }

    @Test
    fun `onDraftSymbolChange - updates symbol when creating`() {
        vm.openCreate()

        vm.onDraftSymbolChange("BTC")

        assertEquals("BTC", vm.state.draftSymbol)
    }

    @Test
    fun `onDraftNameChange - updates name`() {
        vm.openCreate()

        vm.onDraftNameChange("Bitcoin")

        assertEquals("Bitcoin", vm.state.draftName)
    }

    @Test
    fun `onDraftActiveChange - updates active`() {
        vm.openCreate()

        vm.onDraftActiveChange(false)

        assertFalse(vm.state.draftActive)
    }

    @Test
    fun `dismissForm - closes form and clears error`() {
        vm.openCreate()
        // Simula error previo
        vm.state = vm.state.copy(error = "x")

        vm.dismissForm()

        assertFalse(vm.state.showForm)
        assertNull(vm.state.error)
    }

    // -------------------------
    // save()
    // -------------------------

    @Test
    fun `save success create - closes form, updates items, sets created message`() = runTest {
        vm.openCreate()
        vm.onDraftSymbolChange("BTC")
        vm.onDraftNameChange("Bitcoin")
        vm.onDraftActiveChange(true)

        val returnedItems = listOf(
            Crypto(symbol = "BTC", name = "Bitcoin", isActive = true)
        )

        val cmdSlot = slot<UpsertCryptoCommand>()
        coEvery { upsertCrypto.execute(capture(cmdSlot)) } returns
                UpsertCryptoResult.Success(items = returnedItems, wasUpdate = false)

        vm.save()
        advanceUntilIdle()

        // verify command (usa tus fields reales)
        assertEquals("BTC", cmdSlot.captured.symbolRaw)
        assertEquals("Bitcoin", cmdSlot.captured.nameRaw)
        assertTrue(cmdSlot.captured.isActive)
        assertFalse(cmdSlot.captured.isEditing)

        // verify state
        assertFalse(vm.state.loading)
        assertFalse(vm.state.showForm)
        assertNull(vm.state.error)
        assertEquals(returnedItems, vm.state.items)
        assertEquals("Crypto creada.", vm.state.lastActionMessage)

        coVerify(exactly = 1) { upsertCrypto.execute(any()) }
    }

    @Test
    fun `save success update - closes form, updates items, sets updated message`() = runTest {
        val item = Crypto(symbol = "BTC", name = "Bitcoin", isActive = true)
        vm.openEdit(item)
        vm.onDraftNameChange("Bitcoin 2")
        vm.onDraftActiveChange(false)

        val returnedItems = listOf(
            Crypto(symbol = "BTC", name = "Bitcoin 2", isActive = false)
        )

        val cmdSlot = slot<UpsertCryptoCommand>()
        coEvery { upsertCrypto.execute(capture(cmdSlot)) } returns
                UpsertCryptoResult.Success(items = returnedItems, wasUpdate = true)

        vm.save()
        advanceUntilIdle()

        assertEquals("BTC", cmdSlot.captured.symbolRaw)
        assertEquals("Bitcoin 2", cmdSlot.captured.nameRaw)
        assertFalse(cmdSlot.captured.isActive)
        assertTrue(cmdSlot.captured.isEditing)

        assertFalse(vm.state.loading)
        assertFalse(vm.state.showForm)
        assertNull(vm.state.error)
        assertEquals(returnedItems, vm.state.items)
        assertEquals("Crypto actualizada.", vm.state.lastActionMessage)

        coVerify(exactly = 1) { upsertCrypto.execute(any()) }
    }

    @Test
    fun `save validation error - keeps form open and sets error`() = runTest {
        vm.openCreate()
        vm.onDraftSymbolChange("") // inválido, por ejemplo
        vm.onDraftNameChange("Bitcoin")

        coEvery { upsertCrypto.execute(any()) } returns
                UpsertCryptoResult.ValidationError("invalid")

        vm.save()
        advanceUntilIdle()

        assertFalse(vm.state.loading)
        assertTrue(vm.state.showForm) // se mantiene
        assertEquals("invalid", vm.state.error)
        assertNull(vm.state.lastActionMessage)

        coVerify(exactly = 1) { upsertCrypto.execute(any()) }
    }

    @Test
    fun `save failure - keeps form open and sets error`() = runTest {
        vm.openCreate()
        vm.onDraftSymbolChange("BTC")
        vm.onDraftNameChange("Bitcoin")

        coEvery { upsertCrypto.execute(any()) } returns
                UpsertCryptoResult.Failure("fail")

        vm.save()
        advanceUntilIdle()

        assertFalse(vm.state.loading)
        assertTrue(vm.state.showForm)
        assertEquals("fail", vm.state.error)
        assertNull(vm.state.lastActionMessage)

        coVerify(exactly = 1) { upsertCrypto.execute(any()) }
    }

    // -------------------------
    // delete flow
    // -------------------------

    @Test
    fun `requestDelete - sets pending symbol and clears error and message`() {
        vm.state = vm.state.copy(error = "x", lastActionMessage = "y")

        vm.requestDelete("BTC")

        assertEquals("BTC", vm.state.pendingDeleteSymbol)
        assertNull(vm.state.error)
        assertNull(vm.state.lastActionMessage)
    }

    @Test
    fun `cancelDelete - clears pending symbol`() {
        vm.requestDelete("BTC")

        vm.cancelDelete()

        assertNull(vm.state.pendingDeleteSymbol)
    }

    @Test
    fun `confirmDelete deleted - clears pending, updates items, sets deleted message`() = runTest {
        val startItems = listOf(
            Crypto(symbol = "BTC", name = "Bitcoin", isActive = true),
            Crypto(symbol = "ETH", name = "Ethereum", isActive = true)
        )
        vm.state = vm.state.copy(items = startItems)
        vm.requestDelete("BTC")

        val expectedItems = listOf(
            Crypto(symbol = "ETH", name = "Ethereum", isActive = true)
        )

        val cmdSlot = slot<DeleteCryptoCommand>()
        coEvery { deleteCrypto.execute(capture(cmdSlot)) } returns
                DeleteCryptoResult.Deleted(items = expectedItems)

        vm.confirmDelete()
        advanceUntilIdle()

        // OJO: es symbolRaw, no symbol
        assertEquals("BTC", cmdSlot.captured.symbolRaw)

        assertFalse(vm.state.loading)
        assertNull(vm.state.pendingDeleteSymbol)
        assertEquals(expectedItems, vm.state.items)
        assertEquals("Crypto eliminada.", vm.state.lastActionMessage)
        assertNull(vm.state.error)

        coVerify(exactly = 1) { deleteCrypto.execute(any()) }
    }

    @Test
    fun `confirmDelete not found - clears pending, updates items, sets not found message`() = runTest {
        vm.requestDelete("BTC")

        val expectedItems = listOf(
            Crypto(symbol = "ETH", name = "Ethereum", isActive = true)
        )

        coEvery { deleteCrypto.execute(any()) } returns
                DeleteCryptoResult.NotFound(items = expectedItems)

        vm.confirmDelete()
        advanceUntilIdle()

        assertFalse(vm.state.loading)
        assertNull(vm.state.pendingDeleteSymbol)
        assertEquals(expectedItems, vm.state.items)
        assertEquals("No se eliminó (no existía).", vm.state.lastActionMessage)
        assertNull(vm.state.error)

        coVerify(exactly = 1) { deleteCrypto.execute(any()) }
    }

    @Test
    fun `confirmDelete in use - clears pending, keeps items from result, sets error`() = runTest {
        vm.requestDelete("BTC")

        val expectedItems = listOf(
            Crypto(symbol = "BTC", name = "Bitcoin", isActive = true)
        )

        coEvery { deleteCrypto.execute(any()) } returns
                DeleteCryptoResult.InUse(message = "in use", items = expectedItems)

        vm.confirmDelete()
        advanceUntilIdle()

        assertFalse(vm.state.loading)
        assertNull(vm.state.pendingDeleteSymbol)
        assertEquals(expectedItems, vm.state.items)
        assertEquals("in use", vm.state.error)
        assertNull(vm.state.lastActionMessage)

        coVerify(exactly = 1) { deleteCrypto.execute(any()) }
    }

    @Test
    fun `confirmDelete failure - clears pending, keeps items from result, sets error`() = runTest {
        vm.requestDelete("BTC")

        val expectedItems = listOf(
            Crypto(symbol = "BTC", name = "Bitcoin", isActive = true)
        )

        coEvery { deleteCrypto.execute(any()) } returns
                DeleteCryptoResult.Failure(message = "fail", items = expectedItems)

        vm.confirmDelete()
        advanceUntilIdle()

        assertFalse(vm.state.loading)
        assertNull(vm.state.pendingDeleteSymbol)
        assertEquals(expectedItems, vm.state.items)
        assertEquals("fail", vm.state.error)
        assertNull(vm.state.lastActionMessage)

        coVerify(exactly = 1) { deleteCrypto.execute(any()) }
    }

    // -------------------------
    // consumeLastActionMessage()
    // -------------------------

    @Test
    fun `consumeLastActionMessage - clears message`() {
        vm.state = vm.state.copy(lastActionMessage = "ok")

        vm.consumeLastActionMessage()

        assertNull(vm.state.lastActionMessage)
    }
}