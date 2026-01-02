package info.eliumontoyasadec.cryptotracker.domain.interactor.portfolio

import info.eliumontoyasadec.cryptotracker.domain.model.Portfolio

sealed class UpdatePortfolioResult {
    data class Success(val items: List<Portfolio>) : UpdatePortfolioResult()
    data class ValidationError(val message: String) : UpdatePortfolioResult()
    data class Failure(val message: String) : UpdatePortfolioResult()
}