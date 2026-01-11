package info.eliumontoyasadec.cryptotracker.ui.shell

import androidx.compose.runtime.staticCompositionLocalOf
import androidx.lifecycle.ViewModelProvider
import info.eliumontoyasadec.cryptotracker.data.seed.CatalogSeeder
import info.eliumontoyasadec.cryptotracker.data.seed.DatabaseWiper
import info.eliumontoyasadec.cryptotracker.domain.interactor.movement.DeleteMovementUseCase
import info.eliumontoyasadec.cryptotracker.domain.interactor.movement.EditMovementUseCase
import info.eliumontoyasadec.cryptotracker.domain.interactor.movement.LoadMovementsUseCase
import info.eliumontoyasadec.cryptotracker.domain.interactor.movement.MoveBetweenWalletsUseCase
import info.eliumontoyasadec.cryptotracker.domain.interactor.movement.RegisterMovementUseCase
import info.eliumontoyasadec.cryptotracker.domain.interactor.movement.SwapMovementUseCase
import info.eliumontoyasadec.cryptotracker.domain.queries.PortfolioQueries
import info.eliumontoyasadec.cryptotracker.domain.repositories.CryptoRepository
import info.eliumontoyasadec.cryptotracker.domain.repositories.FiatRepository
import info.eliumontoyasadec.cryptotracker.domain.repositories.HoldingRepository
import info.eliumontoyasadec.cryptotracker.domain.repositories.MovementRepository
import info.eliumontoyasadec.cryptotracker.domain.repositories.PortfolioRepository
import info.eliumontoyasadec.cryptotracker.domain.repositories.TransactionRunner
import info.eliumontoyasadec.cryptotracker.domain.repositories.WalletRepository
import info.eliumontoyasadec.cryptotracker.ui.screens.movements.CryptoFilter
import info.eliumontoyasadec.cryptotracker.ui.screens.movements.MovementMode
import info.eliumontoyasadec.cryptotracker.ui.screens.movements.MovementsViewModelFactory


data class AppDeps(
    val portfolioQueries: PortfolioQueries,
    val catalogSeeder: CatalogSeeder,
    val databaseWiper: DatabaseWiper,
    val portfolioRepository: PortfolioRepository,
    val walletRepository: WalletRepository,
    val cryptoRepository: CryptoRepository,
    val fiatRepository: FiatRepository,

//  deps para Movements UseCases (interfaces dominio)
    val movementRepository: MovementRepository,
    val holdingRepository: HoldingRepository,
    val txRunner: TransactionRunner


) {
    /**
     * Wiring manual para MovementsViewModel (productivo).
     * En tests/E2E puedes seguir creando el VM sin pasar este factory.
     */
    fun movementsViewModelFactory(
        mode: MovementMode,
        assetIdResolver: (CryptoFilter) -> String?
    ): ViewModelProvider.Factory {
        val loadUC = LoadMovementsUseCase(
            portfolioRepo = portfolioRepository,
            walletRepo = walletRepository,
            assetRepo = cryptoRepository,
            movementRepo = movementRepository
        )

        val registerUC = RegisterMovementUseCase(
            portfolioRepo = portfolioRepository,
            walletRepo = walletRepository,
            assetRepo = cryptoRepository,
            movementRepo = movementRepository,
            holdingRepo = holdingRepository,
            tx = txRunner
        )
        val editUC = EditMovementUseCase(
            movementRepo = movementRepository,
            holdingRepo = holdingRepository,
            tx = txRunner
        )
        val deleteUC = DeleteMovementUseCase(
            movementRepo = movementRepository,
            holdingRepo = holdingRepository,
            tx = txRunner
        )
        val swapUC = SwapMovementUseCase(
            portfolioRepo = portfolioRepository,
            walletRepo = walletRepository,
            assetRepo = cryptoRepository,
            movementRepo = movementRepository,
            holdingRepo = holdingRepository,
            tx = txRunner
        )
        val moveUC = MoveBetweenWalletsUseCase(
            portfolioRepo = portfolioRepository,
            walletRepo = walletRepository,
            assetRepo = cryptoRepository,
            movementRepo = movementRepository,
            holdingRepo = holdingRepository,
            tx = txRunner
        )

        return MovementsViewModelFactory(
            mode = mode,
            loadMovements = loadUC,
            registerMovement = registerUC,
            editMovement = editUC,
            deleteMovement = deleteUC,
            swapMovement = swapUC,
            moveBetweenWallets = moveUC,
            assetIdResolver = assetIdResolver,
            portfolioRepo = portfolioRepository,
            walletRepo = walletRepository
        )
    }
}
val LocalAppDeps = staticCompositionLocalOf<AppDeps> {
    error("AppDeps not provided. Provide it from MainActivity using CompositionLocalProvider.")
}
