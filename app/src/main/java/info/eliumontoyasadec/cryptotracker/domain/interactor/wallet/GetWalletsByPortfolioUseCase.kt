package info.eliumontoyasadec.cryptotracker.domain.interactor.wallet

import info.eliumontoyasadec.cryptotracker.domain.model.Wallet
import info.eliumontoyasadec.cryptotracker.domain.repositories.WalletRepository

class GetWalletsByPortfolioUseCase(
    private val walletRepo: WalletRepository
) {
    suspend fun execute(cmd: GetWalletsByPortfolioCommand): List<Wallet> =
        walletRepo.getByPortfolio(cmd.portfolioId)
}