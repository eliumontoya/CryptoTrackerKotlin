package info.eliumontoyasadec.cryptotracker.domain.interactor.wallet

import info.eliumontoyasadec.cryptotracker.domain.repositories.PortfolioRepository
import info.eliumontoyasadec.cryptotracker.domain.repositories.WalletRepository

class LoadAdminWalletsContextUseCase(
    private val walletRepo: WalletRepository,
    private val portfolioRepo: PortfolioRepository
) {
    suspend fun execute(): LoadAdminWalletsContextResult {
        return try {
            val portfolios = portfolioRepo.getAll()

            val defaultId = portfolios.firstOrNull { it.isDefault }?.portfolioId
            val ordered = if (defaultId != null) {
                portfolios.sortedByDescending { it.portfolioId == defaultId }
            } else {
                portfolios
            }

            val selectedId = defaultId ?: ordered.firstOrNull()?.portfolioId
            val wallets = if (selectedId != null) walletRepo.getByPortfolio(selectedId) else emptyList()

            LoadAdminWalletsContextResult.Success(
                portfolios = ordered,
                selectedPortfolioId = selectedId,
                wallets = wallets
            )
        } catch (t: Throwable) {
            LoadAdminWalletsContextResult.Failure(t.message ?: "Fallo desconocido")
        }
    }
}