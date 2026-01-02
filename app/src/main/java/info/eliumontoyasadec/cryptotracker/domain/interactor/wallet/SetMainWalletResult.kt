package info.eliumontoyasadec.cryptotracker.domain.interactor.wallet

import info.eliumontoyasadec.cryptotracker.domain.model.Wallet

sealed class SetMainWalletResult {
    data class Success(val items: List<Wallet>) : SetMainWalletResult()
    data class Failure(val message: String) : SetMainWalletResult()
}