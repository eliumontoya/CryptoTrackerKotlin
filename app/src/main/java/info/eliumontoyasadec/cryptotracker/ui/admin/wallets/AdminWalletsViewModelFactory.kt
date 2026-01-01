package info.eliumontoyasadec.cryptotracker.ui.admin.wallets

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import info.eliumontoyasadec.cryptotracker.domain.repositories.PortfolioRepository
import info.eliumontoyasadec.cryptotracker.domain.repositories.WalletRepository

class AdminWalletsViewModelFactory(
    private val walletRepo: WalletRepository,
    private val portfolioRepo: PortfolioRepository
) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(AdminWalletsViewModel::class.java)) {
            return AdminWalletsViewModel(walletRepo, portfolioRepo) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
    }
}