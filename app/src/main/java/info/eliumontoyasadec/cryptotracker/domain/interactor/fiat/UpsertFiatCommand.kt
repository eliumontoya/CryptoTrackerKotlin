package info.eliumontoyasadec.cryptotracker.domain.interactor.fiat

data class UpsertFiatCommand(
    val codeRaw: String,
    val nameRaw: String,
    val symbolRaw: String?,
    val isEditing: Boolean
)