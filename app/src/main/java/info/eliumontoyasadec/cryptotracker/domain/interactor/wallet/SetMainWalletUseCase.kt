package info.eliumontoyasadec.cryptotracker.domain.interactor.wallet

import info.eliumontoyasadec.cryptotracker.domain.repositories.WalletRepository

class SetMainWalletUseCase(
    private val walletRepo: WalletRepository
) {
    suspend fun execute(cmd: SetMainWalletCommand ): SetMainWalletResult {
        val portfolioId = cmd.portfolioId
        val walletId = cmd.walletId

        if (portfolioId <= 0 || walletId <= 0) return SetMainWalletResult.Failure("Datos inválidos.")

        return try {
            walletRepo.setMain(walletId)
            SetMainWalletResult.Success(walletRepo.getByPortfolio(portfolioId))
        } catch (t: Throwable) {
            SetMainWalletResult.Failure(t.message ?: "Fallo desconocido")
        }
    }
}