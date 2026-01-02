package info.eliumontoyasadec.cryptotracker.domain.interactor.crypto

import info.eliumontoyasadec.cryptotracker.domain.model.Crypto

sealed class DeleteCryptoResult {
    data class Deleted(val items: List<Crypto>) : DeleteCryptoResult()
    data class NotFound(val items: List<Crypto>) : DeleteCryptoResult()
    data class InUse(val message: String, val items: List<Crypto>) : DeleteCryptoResult()
    data class Failure(val message: String, val items: List<Crypto>) : DeleteCryptoResult()
}