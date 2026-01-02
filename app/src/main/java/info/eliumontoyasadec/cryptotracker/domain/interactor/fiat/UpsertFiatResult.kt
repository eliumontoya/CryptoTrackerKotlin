package info.eliumontoyasadec.cryptotracker.domain.interactor.fiat

import info.eliumontoyasadec.cryptotracker.domain.model.Fiat

sealed class UpsertFiatResult {
    data class Success(
        val items: List<Fiat>,
        val wasUpdate: Boolean
    ) : UpsertFiatResult()

    data class ValidationError(val message: String) : UpsertFiatResult()
    data class Failure(val message: String) : UpsertFiatResult()
}