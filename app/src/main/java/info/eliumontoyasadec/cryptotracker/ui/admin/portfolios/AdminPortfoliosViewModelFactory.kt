package info.eliumontoyasadec.cryptotracker.ui.admin.portfolios

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import info.eliumontoyasadec.cryptotracker.domain.interactor.portfolio.CreatePortfolioUseCase
import info.eliumontoyasadec.cryptotracker.domain.interactor.portfolio.DeletePortfolioUseCase
import info.eliumontoyasadec.cryptotracker.domain.interactor.portfolio.GetAllPortfoliosUseCase
import info.eliumontoyasadec.cryptotracker.domain.interactor.portfolio.SetDefaultPortfolioUseCase
import info.eliumontoyasadec.cryptotracker.domain.interactor.portfolio.UpdatePortfolioUseCase
import info.eliumontoyasadec.cryptotracker.domain.repositories.PortfolioRepository

class AdminPortfoliosViewModelFactory(
    private val repo: PortfolioRepository
) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        require(modelClass.isAssignableFrom(AdminPortfoliosViewModel::class.java)) {
            "Unknown ViewModel class: ${modelClass.name}"
        }

        return AdminPortfoliosViewModel(
            getAll = GetAllPortfoliosUseCase(repo),
            createPortfolio = CreatePortfolioUseCase(repo),
            updatePortfolio = UpdatePortfolioUseCase(repo),
            deletePortfolio = DeletePortfolioUseCase(repo),
            setDefaultPortfolio = SetDefaultPortfolioUseCase(repo)
        ) as T
    }
}