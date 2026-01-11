package info.eliumontoyasadec.cryptotracker.domain.interactor.movement

import info.eliumontoyasadec.cryptotracker.domain.model.MovementError
import info.eliumontoyasadec.cryptotracker.domain.repositories.CryptoRepository
import info.eliumontoyasadec.cryptotracker.domain.repositories.MovementRepository
import info.eliumontoyasadec.cryptotracker.domain.repositories.PortfolioRepository
import info.eliumontoyasadec.cryptotracker.domain.repositories.WalletRepository

class LoadMovementsUseCase(
    private val portfolioRepo: PortfolioRepository,
    private val walletRepo: WalletRepository,
    private val assetRepo: CryptoRepository,
    private val movementRepo: MovementRepository
) {
    suspend fun execute(cmd: LoadMovementsCommand): LoadMovementsResult {
        if (cmd.portfolioId == 0L) throw MovementError.InvalidInput("portfolioId es requerido")

        if (!portfolioRepo.exists(cmd.portfolioId)) throw MovementError.NotFound("Portafolio no existe")

        cmd.walletId?.let { wid ->
            if (wid == 0L) throw MovementError.InvalidInput("walletId inválido")
            if (!walletRepo.exists(wid)) throw MovementError.NotFound("Wallet no existe")
            if (!walletRepo.belongsToPortfolio(wid, cmd.portfolioId)) {
                throw MovementError.NotAllowed("La wallet no pertenece al portafolio")
            }
        }

        cmd.assetId?.let { aid ->
            if (aid.isBlank()) throw MovementError.InvalidInput("assetId inválido")
            if (!assetRepo.exists(aid)) throw MovementError.NotFound("Asset no existe")
        }

        val items = movementRepo.list(
            portfolioId = cmd.portfolioId,
            walletId = cmd.walletId,
            assetId = cmd.assetId
        )
        return LoadMovementsResult(items)
    }
}