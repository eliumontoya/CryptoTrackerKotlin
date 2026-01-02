package info.eliumontoyasadec.cryptotracker.domain.interactor.portfolio

import info.eliumontoyasadec.cryptotracker.domain.model.Portfolio

sealed class CreatePortfolioResult {
    data class Success(val items: List<Portfolio>) : CreatePortfolioResult()
    data class ValidationError(val message: String) : CreatePortfolioResult()
    data class Failure(val message: String) : CreatePortfolioResult()
}