package info.eliumontoyasadec.cryptotracker.domain.interactor.wallet

import info.eliumontoyasadec.cryptotracker.domain.model.Wallet

sealed class UpsertWalletResult {
    data class Success(val items: List<Wallet>) : UpsertWalletResult()
    data class ValidationError(val message: String) : UpsertWalletResult()
    data class Failure(val message: String) : UpsertWalletResult()
}