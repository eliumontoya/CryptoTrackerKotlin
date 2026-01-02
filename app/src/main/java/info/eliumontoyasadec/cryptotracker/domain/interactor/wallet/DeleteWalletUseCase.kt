package info.eliumontoyasadec.cryptotracker.domain.interactor.wallet

import info.eliumontoyasadec.cryptotracker.domain.repositories.WalletRepository

class DeleteWalletUseCase(
    private val walletRepo: WalletRepository
) {
    suspend fun execute(cmd: DeleteWalletCommand): DeleteWalletResult {
        val portfolioId = cmd.portfolioId
        val walletId = cmd.walletId
        if (portfolioId <= 0 || walletId <= 0) {
            return DeleteWalletResult.Failure("Datos inválidos.", safeGet(portfolioId))
        }

        return try {
            walletRepo.delete(walletId)
            DeleteWalletResult.Success(walletRepo.getByPortfolio(portfolioId))
        } catch (t: Throwable) {
            DeleteWalletResult.Failure(t.message ?: "Fallo desconocido", safeGet(portfolioId))
        }
    }

    private suspend fun safeGet(portfolioId: Long) =
        try { if (portfolioId > 0) walletRepo.getByPortfolio(portfolioId) else emptyList() }
        catch (_: Throwable) { emptyList() }
}