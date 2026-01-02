package info.eliumontoyasadec.cryptotracker.domain.interactor.wallet

data class UpsertWalletCommand(
    val portfolioId: Long,
    val walletId: Long?,      // null => create
    val nameRaw: String,
    val makeMain: Boolean
)