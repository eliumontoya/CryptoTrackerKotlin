package info.eliumontoyasadec.cryptotracker.ui.screens.movements

import info.eliumontoyasadec.cryptotracker.domain.interactor.movement.DeleteMovementCommand
import info.eliumontoyasadec.cryptotracker.domain.interactor.movement.DeleteMovementResult
import info.eliumontoyasadec.cryptotracker.domain.interactor.movement.DeleteMovementUseCase
import info.eliumontoyasadec.cryptotracker.domain.interactor.movement.EditMovementCommand
import info.eliumontoyasadec.cryptotracker.domain.interactor.movement.EditMovementResult
import info.eliumontoyasadec.cryptotracker.domain.interactor.movement.EditMovementUseCase
import info.eliumontoyasadec.cryptotracker.domain.interactor.movement.LoadMovementsResult
import info.eliumontoyasadec.cryptotracker.domain.interactor.movement.LoadMovementsUseCase
import info.eliumontoyasadec.cryptotracker.domain.interactor.movement.MoveBetweenWalletsUseCase
import info.eliumontoyasadec.cryptotracker.domain.interactor.movement.RegisterMovementCommand
import info.eliumontoyasadec.cryptotracker.domain.interactor.movement.RegisterMovementResult
import info.eliumontoyasadec.cryptotracker.domain.interactor.movement.RegisterMovementUseCase
import info.eliumontoyasadec.cryptotracker.domain.interactor.movement.SwapMovementCommand
import info.eliumontoyasadec.cryptotracker.domain.interactor.movement.SwapMovementResult
import info.eliumontoyasadec.cryptotracker.domain.interactor.movement.SwapMovementUseCase
import info.eliumontoyasadec.cryptotracker.domain.model.Movement
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
    private val loadMovements: LoadMovementsUseCase = mockk()

    private lateinit var vm: MovementsViewModel

    // Lista mutable para simular “DB” en tests.
    // Así, cuando el VM refresca con load(), no pierde el movimiento recién creado.
    private val loaded = mutableListOf<Movement>()

    private fun newVm(
        mode: MovementMode,
        register: RegisterMovementUseCase? = registerMovement,
        edit: EditMovementUseCase? = editMovement,
        delete: DeleteMovementUseCase? = deleteMovement,
        swap: SwapMovementUseCase? = swapMovement,
        moveBetween: MoveBetweenWalletsUseCase? = moveBetweenWallets,
        portfolioId: Long = 777L
    ): MovementsViewModel {
        return MovementsViewModel(
            mode = mode,
            loadMovements = loadMovements,
            registerMovement = register,
            editMovement = edit,
            deleteMovement = delete,
            swapMovement = swap,
            moveBetweenWallets = moveBetween,
            portfolioIdProvider = { portfolioId },

            // ✅ Resolver real: enum -> id. ALL -> null.
            walletIdResolver = { wf ->
                when (wf) {
                    WalletFilter.ALL -> null
                    WalletFilter.METAMASK -> 10L
                    WalletFilter.BYBIT -> 20L
                    WalletFilter.PHANTOM -> 30L
                }
            },

            // ✅ Resolver real: enum -> assetId. ALL -> null.
            assetIdResolver = { cf ->
                when (cf) {
                    CryptoFilter.ALL -> null
                    CryptoFilter.BTC -> "BTC"
                    CryptoFilter.ETH -> "ETH"
                    CryptoFilter.SOL -> "SOL"
                    CryptoFilter.ALGO -> "ALGO"
                    CryptoFilter.AIXBT -> "AIXBT"
                }
            }
        )
    }

    // Helpers para evitar smart-cast entre módulos
    private fun MovementsUiState.requireMovementForm(): MovementFormState =
        requireNotNull(movementForm) { "Expected movementForm to be non-null" }

    private fun MovementsUiState.requireSwapForm(): SwapFormState =
        requireNotNull(swapForm) { "Expected swapForm to be non-null" }

    @Before
    fun setUp() = runTest {
        val now = System.currentTimeMillis()

        loaded.clear()
        loaded += Movement(
            id = 1L,
            portfolioId = 777L,
            walletId = 10L,              // METAMASK
            assetId = "BTC",             // BTC
            type = MovementType.DEPOSIT,
            quantity = 0.1,
            price = null,
            feeQuantity = 0.0,
            timestamp = now,
            notes = "seed",
            groupId = null
        )
        loaded += Movement(
            id = 2L,
            portfolioId = 777L,
            walletId = 20L,              // BYBIT
            assetId = "ETH",             // ETH
            type = MovementType.DEPOSIT,
            quantity = 1.5,
            price = null,
            feeQuantity = 0.0,
            timestamp = now - 86_400_000,
            notes = "seed",
            groupId = null
        )

        // ✅ Load dinámico: siempre regresa el estado actual de "loaded"
        coEvery { loadMovements.execute(any()) } answers {
            LoadMovementsResult(items = loaded.toList())
        }

        vm = newVm(MovementMode.IN)
        advanceUntilIdle()
    }

    // -------------------------
    // initial state
    // -------------------------

    @Test
    fun `initial state - has ALL filters and non-empty rows`() = runTest {
        advanceUntilIdle()
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
    fun `selectWallet - updates selectedWallet and filteredRows`() = runTest {
        vm.selectWallet(WalletFilter.METAMASK)
        advanceUntilIdle()

        val st = vm.state.value
        assertEquals(WalletFilter.METAMASK, st.selectedWallet)
        assertTrue(st.filteredRows.isNotEmpty())
        assertTrue(st.filteredRows.all { it.wallet == WalletFilter.METAMASK })
    }

    @Test
    fun `selectCrypto - updates selectedCrypto and filteredRows`() = runTest {
        vm.selectCrypto(CryptoFilter.BTC)
        advanceUntilIdle()

        val st = vm.state.value
        assertEquals(CryptoFilter.BTC, st.selectedCrypto)
        assertTrue(st.filteredRows.isNotEmpty())
        assertTrue(st.filteredRows.all { it.crypto == CryptoFilter.BTC })
    }

    @Test
    fun `selectWallet and selectCrypto - applies AND filter`() = runTest {
        vm.selectWallet(WalletFilter.METAMASK)
        vm.selectCrypto(CryptoFilter.BTC)
        advanceUntilIdle()

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
    fun `startCreate non-swap - opens movement form in CREATE with default type for mode`() = runTest {
        vm = newVm(MovementMode.IN)
        advanceUntilIdle()

        vm.startCreate()

        val st = vm.state.value
        val form = st.requireMovementForm()
        assertNull(st.swapForm)
        assertEquals(MovementFormMode.CREATE, form.mode)
        assertEquals(MovementTypeUi.DEPOSIT, form.draft.type)
    }

    @Test
    fun `startCreate swap - opens swap form and clears movement form`() = runTest {
        vm = newVm(MovementMode.SWAP)
        advanceUntilIdle()

        vm.startCreate()

        val st = vm.state.value
        assertNull(st.movementForm)
        val swap = st.requireSwapForm()
        assertNotNull(swap.draft)
    }

    @Test
    fun `startEdit - opens movement form in EDIT with row mapped to draft`() = runTest {
        advanceUntilIdle()
        val row = vm.state.value.rows.first()

        vm.startEdit(row)

        val st = vm.state.value
        val form = st.requireMovementForm()
        assertEquals(MovementFormMode.EDIT, form.mode)
        assertEquals(row.id, form.draft.id)
        assertEquals(row.wallet, form.draft.wallet)
        assertEquals(row.crypto, form.draft.crypto)
        assertEquals(row.dateLabel, form.draft.dateLabel)
        assertEquals(row.details, form.draft.notes)
        assertNull(st.swapForm)
    }

    @Test
    fun `changeSwapDraft - updates swap draft fields`() = runTest {
        vm = newVm(MovementMode.SWAP)
        advanceUntilIdle()
        vm.startCreate()

        val original = vm.state.value.requireSwapForm().draft
        val updated = original.copy(fromQtyText = "5", toQtyText = "1")

        vm.changeSwapDraft(updated)

        val st = vm.state.value
        val swap = st.requireSwapForm()
        assertEquals("5", swap.draft.fromQtyText)
        assertEquals("1", swap.draft.toQtyText)
    }

    // -------------------------
    // saveMovement - UI-first behavior
    // -------------------------

    @Test
    fun `saveMovement CREATE - inserts optimistic row, closes form`() = runTest {
        vm = newVm(MovementMode.IN, register = null) // solo UI-first
        advanceUntilIdle()
        vm.startCreate()

        val draft = vm.state.value.requireMovementForm().draft.copy(
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
        advanceUntilIdle()

        val st = vm.state.value
        assertNull(st.movementForm)
        assertEquals(beforeRows.size + 1, st.rows.size)
        assertEquals("tmp-1", st.rows.first().id)
        assertEquals(st.rows, st.filteredRows)
    }

    @Test
    fun `saveMovement EDIT - updates optimistic row in place, closes form`() = runTest {
        vm = newVm(MovementMode.IN, edit = null) // solo UI-first
        advanceUntilIdle()

        val row = vm.state.value.rows.first()
        vm.startEdit(row)

        val updated = vm.state.value.requireMovementForm().draft.copy(
            id = row.id,
            quantityText = "0.99",
            notes = "updated"
        )
        vm.changeMovementDraft(updated)

        vm.saveMovement()
        advanceUntilIdle()

        val st = vm.state.value
        assertNull(st.movementForm)
        assertTrue(st.rows.any { it.id == row.id && it.details.contains("updated") })
    }

    // -------------------------
    // saveMovement - productive side effects (Register/Edit)
    // -------------------------

    @Test
    fun `saveMovement CREATE - calls registerMovement with command and re-labels id`() = runTest {
        vm = newVm(mode = MovementMode.IN, portfolioId = 777L)
        advanceUntilIdle()
        vm.startCreate()

        vm.changeMovementDraft(
            vm.state.value.requireMovementForm().draft.copy(
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
        coEvery { registerMovement.execute(capture(cmdSlot)) } answers {
            val cmd = cmdSlot.captured
            val result = RegisterMovementResult(
                movementId = 99L,
                holdingId = "h",
                newHoldingQuantity = 1.0
            )
            // ✅ Actualizamos "DB" para que el refresh traiga id=99
            loaded.add(
                Movement(
                    id = result.movementId,
                    portfolioId = cmd.portfolioId,
                    walletId = cmd.walletId,
                    assetId = cmd.assetId,
                    type = cmd.type,
                    quantity = cmd.quantity,
                    price = cmd.price,
                    feeQuantity = cmd.feeQuantity ?: 0.0,
                    timestamp = cmd.timestamp,
                    notes = cmd.notes,
                    groupId = null
                )
            )
            result
        }

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
    fun `saveMovement EDIT - calls editMovement with command when row id is numeric (after register relabel)`() = runTest {
        vm = newVm(mode = MovementMode.IN)
        advanceUntilIdle()

        // CREATE productivo para obtener id real numérico
        vm.startCreate()
        vm.changeMovementDraft(
            vm.state.value.requireMovementForm().draft.copy(
                id = "tmp-1",
                wallet = WalletFilter.METAMASK,
                crypto = CryptoFilter.BTC,
                type = MovementTypeUi.DEPOSIT,
                quantityText = "1",
                notes = "seed"
            )
        )

        coEvery { registerMovement.execute(any()) } answers {
            val cmd = firstArg<RegisterMovementCommand>()
            val result = RegisterMovementResult(
                movementId = 123L,
                holdingId = "h",
                newHoldingQuantity = 0.0
            )
            loaded.add(
                Movement(
                    id = result.movementId,
                    portfolioId = cmd.portfolioId,
                    walletId = cmd.walletId,
                    assetId = cmd.assetId,
                    type = cmd.type,
                    quantity = cmd.quantity,
                    price = cmd.price,
                    feeQuantity = cmd.feeQuantity ?: 0.0,
                    timestamp = cmd.timestamp,
                    notes = cmd.notes,
                    groupId = null
                )
            )
            result
        }

        vm.saveMovement()
        advanceUntilIdle()

        val createdRow = vm.state.value.rows.first { it.id == "123" }  // en el test EDIT
        assertEquals("123", createdRow.id)

        // EDIT productivo
        vm.startEdit(createdRow)
        vm.changeMovementDraft(
            vm.state.value.requireMovementForm().draft.copy(
                id = createdRow.id,
                type = MovementTypeUi.BUY,
                quantityText = "2.0",
                priceText = "100.5",
                feeQuantityText = "0.01",
                notes = "edit"
            )
        )

        val cmdSlot = slot<EditMovementCommand>()
        coEvery { editMovement.execute(capture(cmdSlot)) } returns EditMovementResult(
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

    // -------------------------
    // saveSwap - UI-first + productive side effects
    // -------------------------

    @Test
    fun `saveSwap - inserts optimistic swap row and closes swap form`() = runTest {
        vm = newVm(MovementMode.SWAP, swap = null) // solo UI-first
        advanceUntilIdle()
        vm.startCreate()

        vm.changeSwapDraft(
            vm.state.value.requireSwapForm().draft.copy(
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
        advanceUntilIdle()

        val st = vm.state.value
        assertNull(st.swapForm)
        assertEquals(before.size + 1, st.rows.size)
        assertEquals("swap-1", st.rows.first().id)
    }

    @Test
    fun `saveSwap - calls swapMovement with command`() = runTest {
        vm = newVm(MovementMode.SWAP)
        advanceUntilIdle()

        vm.startCreate()
        vm.changeSwapDraft(
            vm.state.value.requireSwapForm().draft.copy(
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
    fun `requestDelete - sets pendingDeleteId`() = runTest {
        advanceUntilIdle()
        val row = vm.state.value.rows.first()

        vm.requestDelete(row)

        assertEquals(row.id, vm.state.value.pendingDeleteId)
    }

    @Test
    fun `cancelDelete - clears pendingDeleteId`() = runTest {
        advanceUntilIdle()
        val row = vm.state.value.rows.first()
        vm.requestDelete(row)

        vm.cancelDelete()

        assertNull(vm.state.value.pendingDeleteId)
    }

    @Test
    fun `confirmDelete - removes row and clears pendingDeleteId`() = runTest {
        vm = newVm(MovementMode.IN, delete = null) // solo UI-first
        advanceUntilIdle()

        val row = vm.state.value.rows.first()
        vm.requestDelete(row)

        vm.confirmDelete(row.id)
        advanceUntilIdle()

        val st = vm.state.value
        assertNull(st.pendingDeleteId)
        assertFalse(st.rows.any { it.id == row.id })
    }

    @Test
    fun `confirmDelete - calls deleteMovement when id is numeric (after register relabel)`() = runTest {
        vm = newVm(mode = MovementMode.IN)
        advanceUntilIdle()

        // CREATE productivo para tener id numérico
        vm.startCreate()
        vm.changeMovementDraft(
            vm.state.value.requireMovementForm().draft.copy(
                id = "tmp-del",
                wallet = WalletFilter.METAMASK,
                crypto = CryptoFilter.BTC,
                type = MovementTypeUi.DEPOSIT,
                quantityText = "1",
                notes = "seed"
            )
        )

        coEvery { registerMovement.execute(any()) } answers {
            val cmd = firstArg<RegisterMovementCommand>()
            val result = RegisterMovementResult(
                movementId = 999L,
                holdingId = "h",
                newHoldingQuantity = 0.0
            )
            loaded.add(
                Movement(
                    id = result.movementId,
                    portfolioId = cmd.portfolioId,
                    walletId = cmd.walletId,
                    assetId = cmd.assetId,
                    type = cmd.type,
                    quantity = cmd.quantity,
                    price = cmd.price,
                    feeQuantity = cmd.feeQuantity ?: 0.0,
                    timestamp = cmd.timestamp,
                    notes = cmd.notes,
                    groupId = null
                )
            )
            result
        }

        vm.saveMovement()
        advanceUntilIdle()

        val createdRow = vm.state.value.rows.first { it.id == "999" }
        assertEquals("999", createdRow.id)

        val cmdSlot = slot<DeleteMovementCommand>()
        coEvery { deleteMovement.execute(capture(cmdSlot)) } returns DeleteMovementResult(
            movementId = 999L,
            holdingId = "h",
            newHoldingQuantity = 0.0
        )

        vm.confirmDelete(createdRow.id)
        advanceUntilIdle()

        assertEquals(999L, cmdSlot.captured.movementId)
        coVerify(exactly = 1) { deleteMovement.execute(any()) }
    }

    @Test
    fun `confirmDelete - does NOT call deleteMovement when id is non-numeric and not in idMap`() = runTest {
        vm = newVm(MovementMode.IN)
        advanceUntilIdle()

        vm.confirmDelete("tmp-x")
        advanceUntilIdle()

        coVerify(exactly = 0) { deleteMovement.execute(any()) }
    }
}