package info.eliumontoyasadec.cryptotracker.domain.interactor.portfolio

import info.eliumontoyasadec.cryptotracker.domain.model.Portfolio

sealed class DeletePortfolioResult {
    data class Success(val items: List<Portfolio>) : DeletePortfolioResult()
    data class Failure(val message: String, val items: List<Portfolio>) : DeletePortfolioResult()
}