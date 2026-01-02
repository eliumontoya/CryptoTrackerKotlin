package info.eliumontoyasadec.cryptotracker.domain.interactor.wallet

data class DeleteWalletCommand(
    val portfolioId: Long,
    val walletId: Long
)