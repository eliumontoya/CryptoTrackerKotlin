package info.eliumontoyasadec.cryptotracker.domain.interactor.portfolio

import info.eliumontoyasadec.cryptotracker.domain.model.Portfolio

sealed class SetDefaultPortfolioResult {
    data class Success(val items: List<Portfolio>) : SetDefaultPortfolioResult()
    data class Failure(val message: String) : SetDefaultPortfolioResult()
}