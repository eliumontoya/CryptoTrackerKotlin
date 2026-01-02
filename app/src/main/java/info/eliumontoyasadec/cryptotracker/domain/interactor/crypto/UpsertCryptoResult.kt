package info.eliumontoyasadec.cryptotracker.domain.interactor.crypto

import info.eliumontoyasadec.cryptotracker.domain.model.Crypto

sealed class UpsertCryptoResult {
    data class Success(
        val items: List<Crypto>,
        val wasUpdate: Boolean
    ) : UpsertCryptoResult()

    data class ValidationError(val message: String) : UpsertCryptoResult()
    data class Failure(val message: String) : UpsertCryptoResult()
}