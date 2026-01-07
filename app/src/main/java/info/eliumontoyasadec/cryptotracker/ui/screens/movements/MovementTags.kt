package info.eliumontoyasadec.cryptotracker.ui.screens.movements

object MovementTags {

    // Screen
    const val Screen = "movements:screen"

    // List + create entry point
    const val List = "movements:list"
    const val AddButton = "movements:add"

    // Row (dinámico por id)
    fun row(id: String) = "movements:row:$id"
    fun rowMenu(id: String) = "movements:row:$id:menu"
    fun rowEdit(id: String) = "movements:row:$id:edit"
    fun rowDelete(id: String) = "movements:row:$id:delete"

    // Delete dialog
    const val DeleteDialog = "movements:delete_dialog"
    const val DeleteConfirm = "movements:delete_confirm"
    const val DeleteCancel = "movements:delete_cancel"

    // Form sheet
    const val FormSheet = "movements:form_sheet"
    const val FormSheetContainer = "movements:form_sheet_container"

    const val FormTypeChips = "movements:form:type_chips"
    fun formTypeChip(typeName: String) = "movements:form:type:$typeName"

    const val FormQuantity = "movements:form:quantity"
    const val FormPrice = "movements:form:price"
    const val FormFee = "movements:form:fee"
    const val FormNotes = "movements:form:notes"
    const val FormPickDate = "movements:form:pick_date"

    const val FormCancel = "movements:form:cancel"
    const val FormSave = "movements:form:save"
}