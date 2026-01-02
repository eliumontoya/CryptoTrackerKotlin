package info.eliumontoyasadec.cryptotracker.domain.interactor.wallet

import info.eliumontoyasadec.cryptotracker.domain.model.Portfolio
import info.eliumontoyasadec.cryptotracker.domain.model.Wallet

sealed class LoadAdminWalletsContextResult {
    data class Success(
        val portfolios: List<Portfolio>,
        val selectedPortfolioId: Long?,
        val wallets: List<Wallet>
    ) : LoadAdminWalletsContextResult()

    data class Failure(val message: String) : LoadAdminWalletsContextResult()
}