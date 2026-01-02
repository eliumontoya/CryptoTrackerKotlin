package info.eliumontoyasadec.cryptotracker.ui.admin.wallets

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import info.eliumontoyasadec.cryptotracker.domain.interactor.wallet.DeleteWalletUseCase
import info.eliumontoyasadec.cryptotracker.domain.interactor.wallet.GetWalletsByPortfolioUseCase
import info.eliumontoyasadec.cryptotracker.domain.interactor.wallet.LoadAdminWalletsContextUseCase
import info.eliumontoyasadec.cryptotracker.domain.interactor.wallet.SetMainWalletUseCase
import info.eliumontoyasadec.cryptotracker.domain.interactor.wallet.UpsertWalletUseCase
import info.eliumontoyasadec.cryptotracker.domain.repositories.PortfolioRepository
import info.eliumontoyasadec.cryptotracker.domain.repositories.WalletRepository

class AdminWalletsViewModelFactory(
    private val walletRepo: WalletRepository,
    private val portfolioRepo: PortfolioRepository
) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        require(modelClass.isAssignableFrom(AdminWalletsViewModel::class.java)) {
            "Unknown ViewModel class: ${modelClass.name}"
        }

        return AdminWalletsViewModel(
            loadContext = LoadAdminWalletsContextUseCase(walletRepo, portfolioRepo),
            getWalletsByPortfolio = GetWalletsByPortfolioUseCase(walletRepo),
            upsertWallet = UpsertWalletUseCase(walletRepo),
            deleteWallet = DeleteWalletUseCase(walletRepo),
            setMainWallet = SetMainWalletUseCase(walletRepo)
        ) as T
    }
}