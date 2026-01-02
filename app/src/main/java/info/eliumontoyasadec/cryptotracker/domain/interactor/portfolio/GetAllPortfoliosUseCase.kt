package info.eliumontoyasadec.cryptotracker.domain.interactor.portfolio

import info.eliumontoyasadec.cryptotracker.domain.model.Portfolio
import info.eliumontoyasadec.cryptotracker.domain.repositories.PortfolioRepository

class GetAllPortfoliosUseCase(
    private val repo: PortfolioRepository
) {
    suspend fun execute(): List<Portfolio> = repo.getAll()
}