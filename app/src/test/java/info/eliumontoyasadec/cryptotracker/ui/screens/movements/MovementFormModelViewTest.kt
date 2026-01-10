package info.eliumontoyasadec.cryptotracker.ui.screens.movements

import io.mockk.mockk
import io.mockk.verify
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class MovementFormModelViewTest {

    private val onCancelExternal: () -> Unit = mockk(relaxed = true)
    private val onDraftChangeExternal: (MovementDraft) -> Unit = mockk(relaxed = true)
    private val onSaveExternal: () -> Unit = mockk(relaxed = true)

    private fun newMv(
        mode: MovementFormMode = MovementFormMode.CREATE,
        draft: MovementDraft = MovementDraft()
    ): MovementFormModelView {
        return MovementFormModelView(
            initialMode = mode,
            initialDraft = draft,
            onCancelExternal = onCancelExternal,
            onDraftChangeExternal = onDraftChangeExternal,
            onSaveExternal = onSaveExternal
        )
    }

    private lateinit var mv: MovementFormModelView

    @Before
    fun setUp() {
        mv = newMv()
    }

    // -------------------------
    // initial validation
    // -------------------------

    @Test
    fun `initial state - quantity required, cannot save`() {
        val st = mv.state

        assertEquals(MovementFormMode.CREATE, st.mode)
        assertEquals("", st.draft.quantityText)
        assertEquals("Requerido", st.quantityError)
        assertNull(st.priceError)
        assertNull(st.feeError)
        assertFalse(st.canSave)
        assertFalse(st.showDatePicker)
        assertNull(st.selectedDateMillis)
    }

    @Test
    fun `valid quantity only - can save`() {
        mv.onQuantityChange("1.0")

        val st = mv.state
        assertNull(st.quantityError)
        assertTrue(st.canSave)
    }

    @Test
    fun `invalid quantity - cannot save and shows error`() {
        mv.onQuantityChange("abc")

        val st = mv.state
        assertEquals("Cantidad inválida", st.quantityError)
        assertFalse(st.canSave)
    }

    @Test
    fun `negative quantity - cannot save and shows error`() {
        mv.onQuantityChange("-1")

        val st = mv.state
        assertEquals("Cantidad inválida", st.quantityError)
        assertFalse(st.canSave)
    }

    @Test
    fun `invalid price - blocks save and shows price error`() {
        mv.onQuantityChange("1") // qty ok
        mv.onPriceChange("nope")

        val st = mv.state
        assertNull(st.quantityError)
        assertEquals("Precio inválido", st.priceError)
        assertFalse(st.canSave)
    }

    @Test
    fun `negative price - blocks save and shows price error`() {
        mv.onQuantityChange("1") // qty ok
        mv.onPriceChange("-0.1")

        val st = mv.state
        assertEquals("Precio inválido", st.priceError)
        assertFalse(st.canSave)
    }

    @Test
    fun `blank price - allowed`() {
        mv.onQuantityChange("1") // qty ok
        mv.onPriceChange("")

        val st = mv.state
        assertNull(st.priceError)
        assertTrue(st.canSave)
    }

    @Test
    fun `invalid fee - blocks save and shows fee error`() {
        mv.onQuantityChange("1") // qty ok
        mv.onFeeChange("x")

        val st = mv.state
        assertEquals("Fee inválido", st.feeError)
        assertFalse(st.canSave)
    }

    @Test
    fun `negative fee - blocks save and shows fee error`() {
        mv.onQuantityChange("1") // qty ok
        mv.onFeeChange("-0.01")

        val st = mv.state
        assertEquals("Fee inválido", st.feeError)
        assertFalse(st.canSave)
    }

    @Test
    fun `blank fee - allowed`() {
        mv.onQuantityChange("1") // qty ok
        mv.onFeeChange("")

        val st = mv.state
        assertNull(st.feeError)
        assertTrue(st.canSave)
    }

    // -------------------------
    // callbacks sync
    // -------------------------

    @Test
    fun `onCancel - delegates to external callback`() {
        mv.onCancel()
        verify(exactly = 1) { onCancelExternal.invoke() }
    }

    @Test
    fun `onDraftChange - updates local state and calls external sync`() {
        val updated = mv.state.draft.copy(
            wallet = WalletFilter.METAMASK,
            crypto = CryptoFilter.BTC,
            type = MovementTypeUi.DEPOSIT,
            quantityText = "2",
            priceText = "10",
            feeQuantityText = "0.1",
            notes = "n"
        )

        mv.onDraftChange(updated)

        val st = mv.state
        assertEquals(updated, st.draft)
        assertTrue(st.canSave)

        verify(exactly = 1) { onDraftChangeExternal.invoke(updated) }
    }

    @Test
    fun `field helpers - update draft and call external sync`() {
        mv.onQuantityChange("3")
        mv.onPriceChange("100")
        mv.onFeeChange("0.01")
        mv.onNotesChange("hello")
        mv.onWalletSelect(WalletFilter.METAMASK)
        mv.onCryptoSelect(CryptoFilter.BTC)
        mv.onTypeSelect(MovementTypeUi.BUY)

        val st = mv.state
        assertEquals("3", st.draft.quantityText)
        assertEquals("100", st.draft.priceText)
        assertEquals("0.01", st.draft.feeQuantityText)
        assertEquals("hello", st.draft.notes)
        assertEquals(WalletFilter.METAMASK, st.draft.wallet)
        assertEquals(CryptoFilter.BTC, st.draft.crypto)
        assertEquals(MovementTypeUi.BUY, st.draft.type)

        // Cada helper usa onDraftChange(...) => sync externo debe haberse llamado al menos una vez
        verify(atLeast = 1) { onDraftChangeExternal.invoke(any()) }
    }

    // -------------------------
    // save behavior (guarded by canSave)
    // -------------------------

    @Test
    fun `onSave - does nothing when cannot save`() {
        // initial quantity empty => canSave false
        assertFalse(mv.state.canSave)

        mv.onSave()

        verify(exactly = 0) { onSaveExternal.invoke() }
    }

    @Test
    fun `onSave - calls external when canSave is true`() {
        mv.onQuantityChange("1")
        assertTrue(mv.state.canSave)

        mv.onSave()

        verify(exactly = 1) { onSaveExternal.invoke() }
    }

    // -------------------------
    // date picker flow
    // -------------------------

    @Test
    fun `openDatePicker - sets showDatePicker true`() {
        mv.openDatePicker()
        assertTrue(mv.state.showDatePicker)
    }

    @Test
    fun `dismissDatePicker - sets showDatePicker false`() {
        mv.openDatePicker()
        assertTrue(mv.state.showDatePicker)

        mv.dismissDatePicker()

        assertFalse(mv.state.showDatePicker)
    }

    @Test
    fun `onDatePicked null - hides date picker and keeps selectedDateMillis`() {
        mv.openDatePicker()
        mv.onDatePicked(null)

        val st = mv.state
        assertFalse(st.showDatePicker)
        // selectedDateMillis no debe forzarse a algo
        // (si venía null, se queda null)
        assertNull(st.selectedDateMillis)
    }

    @Test
    fun `onDatePicked millis - updates dateLabel, selectedDateMillis and closes picker`() {
        mv.onQuantityChange("1") // para que canSave esté true y no meta ruido la validación
        mv.openDatePicker()

        val millis = 1_700_000_000_000L
        val beforeLabel = mv.state.draft.dateLabel

        mv.onDatePicked(millis)

        val st = mv.state
        assertFalse(st.showDatePicker)
        assertEquals(millis, st.selectedDateMillis)
        assertNotNull(st.draft.dateLabel)
        assertTrue(st.draft.dateLabel.isNotBlank())
        assertTrue(st.draft.dateLabel != beforeLabel)

        // Importante: onDatePicked también sincroniza draft hacia afuera
        verify(atLeast = 1) { onDraftChangeExternal.invoke(any()) }
    }
}