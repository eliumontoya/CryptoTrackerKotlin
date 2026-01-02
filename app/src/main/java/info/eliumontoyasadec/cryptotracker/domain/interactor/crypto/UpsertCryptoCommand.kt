package info.eliumontoyasadec.cryptotracker.domain.interactor.crypto

data class UpsertCryptoCommand(
    val symbolRaw: String,
    val nameRaw: String,
    val isActive: Boolean,
    val isEditing: Boolean
)