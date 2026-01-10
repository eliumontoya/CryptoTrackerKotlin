package info.eliumontoyasadec.cryptotracker.ui.screens.movements

import info.eliumontoyasadec.cryptotracker.domain.interactor.movement.DeleteMovementCommand
import info.eliumontoyasadec.cryptotracker.domain.interactor.movement.DeleteMovementUseCase
import info.eliumontoyasadec.cryptotracker.domain.interactor.movement.EditMovementCommand
import info.eliumontoyasadec.cryptotracker.domain.interactor.movement.EditMovementUseCase
import info.eliumontoyasadec.cryptotracker.domain.interactor.movement.MoveBetweenWalletsUseCase
import info.eliumontoyasadec.cryptotracker.domain.interactor.movement.RegisterMovementCommand
import info.eliumontoyasadec.cryptotracker.domain.interactor.movement.RegisterMovementResult
import info.eliumontoyasadec.cryptotracker.domain.interactor.movement.RegisterMovementUseCase
import info.eliumontoyasadec.cryptotracker.domain.interactor.movement.SwapMovementCommand
import info.eliumontoyasadec.cryptotracker.domain.interactor.movement.SwapMovementResult
import info.eliumontoyasadec.cryptotracker.domain.interactor.movement.SwapMovementUseCase
import info.eliumontoyasadec.cryptotracker.domain.model.MovementType
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
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class MovementsViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val registerMovement: RegisterMovementUseCase = mockk()
    private val editMovement: EditMovementUseCase = mockk()
    private val deleteMovement: DeleteMovementUseCase = mockk()
    private val swapMovement: SwapMovementUseCase = mockk()
    private val moveBetweenWallets: MoveBetweenWalletsUseCase = mockk()

    private fun newVm(
        mode: MovementMode,
        register: RegisterMovementUseCase? = registerMovement,
        edit: EditMovementUseCase? = editMovement,
        delete: DeleteMovementUseCase? = deleteMovement,
        swap: SwapMovementUseCase? = swapMovement,
        moveBetween: MoveBetweenWalletsUseCase? = moveBetweenWallets,
        portfolioId: Long = 777L,
        walletId: Long = 10L,
        assetId: String = "BTC"
    ): MovementsViewModel {
        return MovementsViewModel(
            mode = mode,
            registerMovement = register,
            editMovement = edit,
            deleteMovement = delete,
            swapMovement = swap,
            moveBetweenWallets = moveBetween,
            portfolioIdProvider = { portfolioId },
            walletIdResolver = { walletId },
            assetIdResolver = { assetId }
        )
    }

    private lateinit var vm: MovementsViewModel

    @Before
    fun setUp() {
        vm = newVm(MovementMode.IN)
    }

    // -------------------------
    // initial state
    // -------------------------

    @Test
    fun `initial state - has ALL filters and non-empty rows`() {
        val st = vm.state.value
        assertEquals(WalletFilter.ALL, st.selectedWallet)
        assertEquals(CryptoFilter.ALL, st.selectedCrypto)
        assertTrue(st.rows.isNotEmpty())
        assertEquals(st.rows, st.filteredRows)
        assertNull(st.movementForm)
        assertNull(st.swapForm)
        assertNull(st.pendingDeleteId)
    }

    // -------------------------
    // filters
    // -------------------------

    @Test
    fun `selectWallet - updates selectedWallet and filteredRows`() {
        vm.selectWallet(WalletFilter.METAMASK)

        val st = vm.state.value
        assertEquals(WalletFilter.METAMASK, st.selectedWallet)
        assertTrue(st.filteredRows.isNotEmpty())
        assertTrue(st.filteredRows.all { it.wallet == WalletFilter.METAMASK })
    }

    @Test
    fun `selectCrypto - updates selectedCrypto and filteredRows`() {
        vm.selectCrypto(CryptoFilter.BTC)

        val st = vm.state.value
        assertEquals(CryptoFilter.BTC, st.selectedCrypto)
        assertTrue(st.filteredRows.isNotEmpty())
        assertTrue(st.filteredRows.all { it.crypto == CryptoFilter.BTC })
    }

    @Test
    fun `selectWallet and selectCrypto - applies AND filter`() {
        vm.selectWallet(WalletFilter.METAMASK)
        vm.selectCrypto(CryptoFilter.BTC)

        val st = vm.state.value
        assertEquals(WalletFilter.METAMASK, st.selectedWallet)
        assertEquals(CryptoFilter.BTC, st.selectedCrypto)
        assertTrue(st.filteredRows.isNotEmpty())
        assertTrue(st.filteredRows.all { it.wallet == WalletFilter.METAMASK && it.crypto == CryptoFilter.BTC })
    }

    // -------------------------
    // forms open/dismiss/change
    // -------------------------

    @Test
    fun `startCreate non-swap - opens movement form in CREATE with default type for mode`() {
        vm = newVm(MovementMode.IN)

        vm.startCreate()

        val st = vm.state.value
        assertNotNull(st.movementForm)
        assertNull(st.swapForm)
        assertEquals(MovementFormMode.CREATE, st.movementForm!!.mode)
        assertEquals(MovementTypeUi.DEPOSIT, st.movementForm.draft.type)
    }

    @Test
    fun `startCreate swap - opens swap form and clears movement form`() {
        vm = newVm(MovementMode.SWAP)

        vm.startCreate()

        val st = vm.state.value
        assertNull(st.movementForm)
        assertNotNull(st.swapForm)
        assertNotNull(st.swapForm!!.draft)
    }

    @Test
    fun `startEdit - opens movement form in EDIT with row mapped to draft`() {
        val row = vm.state.value.rows.first()

        vm.startEdit(row)

        val st = vm.state.value
        assertNotNull(st.movementForm)
        assertEquals(MovementFormMode.EDIT, st.movementForm!!.mode)
        assertEquals(row.id, st.movementForm.draft.id)
        assertEquals(row.wallet, st.movementForm.draft.wallet)
        assertEquals(row.crypto, st.movementForm.draft.crypto)
        assertEquals(row.dateLabel, st.movementForm.draft.dateLabel)
        assertEquals(row.details, st.movementForm.draft.notes)
        assertNull(st.swapForm)
    }

    @Test
    fun `dismissForms - clears movementForm and swapForm`() {
        vm.startCreate()
        assertNotNull(vm.state.value.movementForm)

        vm.dismissForms()

        val st = vm.state.value
        assertNull(st.movementForm)
        assertNull(st.swapForm)
    }

    @Test
    fun `changeMovementDraft - updates draft when form exists`() {
        vm.startCreate()
        val original = vm.state.value.movementForm!!.draft
        val updated = original.copy(quantityText = "1.23", priceText = "100")

        vm.changeMovementDraft(updated)

        val st = vm.state.value
        assertEquals("1.23", st.movementForm!!.draft.quantityText)
        assertEquals("100", st.movementForm.draft.priceText)
    }

    @Test
    fun `changeMovementDraft - no-op when form is null`() {
        val before = vm.state.value

        vm.changeMovementDraft(MovementDraft(quantityText = "9"))

        val after = vm.state.value
        assertEquals(before, after)
    }

    @Test
    fun `changeSwapDraft - updates draft when swap form exists`() {
        vm = newVm(MovementMode.SWAP)
        vm.startCreate()

        val original = vm.state.value.swapForm!!.draft
        val updated = original.copy(fromQtyText = "5", toQtyText = "1")

        vm.changeSwapDraft(updated)

        val st = vm.state.value
        assertEquals("5", st.swapForm!!.draft.fromQtyText)
        assertEquals("1", st.swapForm.draft.toQtyText)
    }

    // -------------------------
    // saveMovement - UI-first behavior
    // -------------------------

    @Test
    fun `saveMovement CREATE - inserts optimistic row, closes form`() {
        vm = newVm(MovementMode.IN, register = null) // solo UI-first
        vm.startCreate()

        val draft = vm.state.value.movementForm!!.draft.copy(
            id = "tmp-1",
            wallet = WalletFilter.METAMASK,
            crypto = CryptoFilter.BTC,
            type = MovementTypeUi.DEPOSIT,
            quantityText = "0.5",
            priceText = "",
            feeQuantityText = "",
            notes = "n"
        )
        vm.changeMovementDraft(draft)

        val beforeRows = vm.state.value.rows

        vm.saveMovement()

        val st = vm.state.value
        assertNull(st.movementForm)
        assertEquals(beforeRows.size + 1, st.rows.size)
        assertEquals("tmp-1", st.rows.first().id)
        assertEquals(st.rows, st.filteredRows)
    }

    @Test
    fun `saveMovement EDIT - updates optimistic row in place, closes form`() {
        vm = newVm(MovementMode.IN, edit = null) // solo UI-first
        val row = vm.state.value.rows.first()

        vm.startEdit(row)
        val updated = vm.state.value.movementForm!!.draft.copy(
            quantityText = "0.99",
            notes = "updated"
        )
        vm.changeMovementDraft(updated)

        vm.saveMovement()

        val st = vm.state.value
        assertNull(st.movementForm)
        assertTrue(st.rows.any { it.id == row.id && it.details.contains("updated") })
    }

    // -------------------------
    // saveMovement - productive side effects (Register/Edit)
    // -------------------------

    @Test
    fun `saveMovement CREATE - calls registerMovement with command and re-labels id`() = runTest {
        vm = newVm(
            mode = MovementMode.IN,
            portfolioId = 777L,
            walletId = 10L,
            assetId = "BTC"
        )
        vm.startCreate()

        vm.changeMovementDraft(
            vm.state.value.movementForm!!.draft.copy(
                id = "tmp-1",
                wallet = WalletFilter.METAMASK,
                crypto = CryptoFilter.BTC,
                type = MovementTypeUi.DEPOSIT,
                quantityText = "1.5",
                priceText = "",
                feeQuantityText = "0.1",
                notes = "note"
            )
        )

        val cmdSlot = slot<RegisterMovementCommand>()
        coEvery { registerMovement.execute(capture(cmdSlot)) } returns RegisterMovementResult(
            movementId = 99L,
            holdingId = "h",
            newHoldingQuantity = 1.0
        )

        vm.saveMovement()
        advanceUntilIdle()

        assertEquals(777L, cmdSlot.captured.portfolioId)
        assertEquals(10L, cmdSlot.captured.walletId)
        assertEquals("BTC", cmdSlot.captured.assetId)
        assertEquals(MovementType.DEPOSIT, cmdSlot.captured.type)
        assertEquals(1.5, cmdSlot.captured.quantity, 0.000001)
        assertNull(cmdSlot.captured.price)
        assertEquals(0.1, cmdSlot.captured.feeQuantity!!, 0.000001)
        assertEquals("note", cmdSlot.captured.notes)

        val st = vm.state.value
        assertTrue(st.rows.any { it.id == "99" })
        assertFalse(st.rows.any { it.id == "tmp-1" })

        coVerify(exactly = 1) { registerMovement.execute(any()) }
    }

    @Test
    fun `saveMovement EDIT - calls editMovement with command when draft id is numeric`() = runTest {
        vm = newVm(mode = MovementMode.IN)
        val row = vm.state.value.rows.first().copy(id = "123")

        vm.startEdit(row)
        vm.changeMovementDraft(
            vm.state.value.movementForm!!.draft.copy(
                type = MovementTypeUi.BUY,
                quantityText = "2.0",
                priceText = "100.5",
                feeQuantityText = "0.01",
                notes = "edit"
            )
        )

        val cmdSlot = slot<EditMovementCommand>()
        coEvery { editMovement.execute(capture(cmdSlot)) } returns
                info.eliumontoyasadec.cryptotracker.domain.interactor.movement.EditMovementResult(
                    movementId = 123L,
                    holdingId = "h",
                    newHoldingQuantity = 0.0
                )

        vm.saveMovement()
        advanceUntilIdle()

        assertEquals(123L, cmdSlot.captured.movementId)
        assertEquals(MovementType.BUY, cmdSlot.captured.newType)
        assertEquals(2.0, cmdSlot.captured.newQuantity, 0.000001)
        assertEquals(100.5, cmdSlot.captured.newPrice!!, 0.000001)
        assertEquals(0.01, cmdSlot.captured.newFeeQuantity!!, 0.000001)
        assertEquals("edit", cmdSlot.captured.newNotes)

        coVerify(exactly = 1) { editMovement.execute(any()) }
    }

    @Test
    fun `saveMovement EDIT - does NOT call editMovement when movementId cannot be resolved`() = runTest {
        vm = newVm(mode = MovementMode.IN)
        val row = vm.state.value.rows.first().copy(id = "tmp-x")
        vm.startEdit(row)

        vm.changeMovementDraft(
            vm.state.value.movementForm!!.draft.copy(
                quantityText = "1",
                notes = "x"
            )
        )

        vm.saveMovement()
        advanceUntilIdle()

        coVerify(exactly = 0) { editMovement.execute(any()) }
    }

    // -------------------------
    // saveSwap - UI-first + productive side effects
    // -------------------------

    @Test
    fun `saveSwap - inserts optimistic swap row and closes swap form`() {
        vm = newVm(MovementMode.SWAP, swap = null) // solo UI-first
        vm.startCreate()

        vm.changeSwapDraft(
            vm.state.value.swapForm!!.draft.copy(
                id = "swap-1",
                wallet = WalletFilter.METAMASK,
                fromCrypto = CryptoFilter.ALGO,
                toCrypto = CryptoFilter.AIXBT,
                fromQtyText = "5",
                toQtyText = "1"
            )
        )

        val before = vm.state.value.rows
        vm.saveSwap()

        val st = vm.state.value
        assertNull(st.swapForm)
        assertEquals(before.size + 1, st.rows.size)
        assertEquals("swap-1", st.rows.first().id)
    }

    @Test
    fun `saveSwap - calls swapMovement with command`() = runTest {
        vm = MovementsViewModel(
            mode = MovementMode.SWAP,
            registerMovement = null,
            editMovement = null,
            deleteMovement = null,
            swapMovement = swapMovement,
            moveBetweenWallets = moveBetweenWallets,
            portfolioIdProvider = { 777L },
            walletIdResolver = { 10L },
            assetIdResolver = {
                when (it) {
                    CryptoFilter.ALGO -> "ALGO"
                    CryptoFilter.AIXBT -> "AIXBT"
                    CryptoFilter.BTC -> "BTC"
                    CryptoFilter.ETH -> "ETH"
                    CryptoFilter.SOL -> "SOL"
                    CryptoFilter.ALL -> null
                }
            }
        )

        vm.startCreate()
        vm.changeSwapDraft(
            vm.state.value.swapForm!!.draft.copy(
                id = "swap-1",
                wallet = WalletFilter.METAMASK,
                fromCrypto = CryptoFilter.ALGO,
                toCrypto = CryptoFilter.AIXBT,
                fromQtyText = "5",
                toQtyText = "1"
            )
        )

        val cmdSlot = slot<SwapMovementCommand>()
        coEvery { swapMovement.execute(capture(cmdSlot)) } returns SwapMovementResult(
            groupId = 1L,
            sellMovementId = 2L,
            buyMovementId = 3L,
            fromHoldingId = "h1",
            toHoldingId = "h2",
            newFromHoldingQuantity = 0.0,
            newToHoldingQuantity = 0.0
        )

        vm.saveSwap()
        advanceUntilIdle()

        assertEquals(777L, cmdSlot.captured.portfolioId)
        assertEquals(10L, cmdSlot.captured.walletId)
        assertEquals("ALGO", cmdSlot.captured.fromAssetId)
        assertEquals("AIXBT", cmdSlot.captured.toAssetId)
        assertEquals(5.0, cmdSlot.captured.fromQuantity, 0.000001)
        assertEquals(1.0, cmdSlot.captured.toQuantity, 0.000001)

        coVerify(exactly = 1) { swapMovement.execute(any()) }
    }

    // -------------------------
    // delete flow
    // -------------------------

    @Test
    fun `requestDelete - sets pendingDeleteId`() {
        val row = vm.state.value.rows.first()

        vm.requestDelete(row)

        assertEquals(row.id, vm.state.value.pendingDeleteId)
    }

    @Test
    fun `cancelDelete - clears pendingDeleteId`() {
        val row = vm.state.value.rows.first()
        vm.requestDelete(row)

        vm.cancelDelete()

        assertNull(vm.state.value.pendingDeleteId)
    }

    @Test
    fun `confirmDelete - removes row and clears pendingDeleteId`() {
        vm = newVm(MovementMode.IN, delete = null) // solo UI-first
        val row = vm.state.value.rows.first()
        vm.requestDelete(row)

        vm.confirmDelete(row.id)

        val st = vm.state.value
        assertNull(st.pendingDeleteId)
        assertFalse(st.rows.any { it.id == row.id })
    }

    @Test
    fun `confirmDelete - calls deleteMovement when id is numeric`() = runTest {
        vm = newVm(MovementMode.IN)

        val cmdSlot = slot<DeleteMovementCommand>()
        coEvery { deleteMovement.execute(capture(cmdSlot)) } returns
                info.eliumontoyasadec.cryptotracker.domain.interactor.movement.DeleteMovementResult(
                    movementId = 999L,
                    holdingId = "h",
                    newHoldingQuantity = 0.0
                )

        vm.confirmDelete("999")
        advanceUntilIdle()

        assertEquals(999L, cmdSlot.captured.movementId)
        coVerify(exactly = 1) { deleteMovement.execute(any()) }
    }

    @Test
    fun `confirmDelete - does NOT call deleteMovement when id is non-numeric and not in idMap`() = runTest {
        vm = newVm(MovementMode.IN)

        vm.confirmDelete("tmp-x")
        advanceUntilIdle()

        coVerify(exactly = 0) { deleteMovement.execute(any()) }
    }
}