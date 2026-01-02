package info.eliumontoyasadec.cryptotracker.domain.interactor.fiat

import info.eliumontoyasadec.cryptotracker.domain.model.Fiat

sealed class DeleteFiatResult {
    data class Deleted(val items: List<Fiat>) : DeleteFiatResult()
    data class NotFound(val items: List<Fiat>) : DeleteFiatResult()
    data class Failure(val message: String, val items: List<Fiat>) : DeleteFiatResult()
}