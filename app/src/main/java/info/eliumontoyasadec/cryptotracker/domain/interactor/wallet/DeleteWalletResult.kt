package info.eliumontoyasadec.cryptotracker.domain.interactor.wallet

import info.eliumontoyasadec.cryptotracker.domain.model.Wallet

sealed class DeleteWalletResult {
    data class Success(val items: List<Wallet>) : DeleteWalletResult()
    data class Failure(val message: String, val items: List<Wallet>) : DeleteWalletResult()
}