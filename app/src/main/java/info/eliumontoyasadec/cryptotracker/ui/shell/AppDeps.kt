package info.eliumontoyasadec.cryptotracker.ui.shell

import androidx.compose.runtime.staticCompositionLocalOf
import androidx.lifecycle.ViewModelProvider
import info.eliumontoyasadec.cryptotracker.data.seed.CatalogSeeder
import info.eliumontoyasadec.cryptotracker.data.seed.DatabaseWiper
import info.eliumontoyasadec.cryptotracker.domain.interactor.movement.*
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
import info.eliumontoyasadec.cryptotracker.ui.screens.movements.WalletFilter
import info.eliumontoyasadec.cryptotracker.ui.screens.movements.defaultMovementAssetId
import info.eliumontoyasadec.cryptotracker.ui.screens.movements.defaultMovementWalletId


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
        portfolioIdProvider: () -> Long = { 1L },
        walletIdResolver: (WalletFilter) -> Long? = ::defaultMovementWalletId,
        assetIdResolver: (CryptoFilter) -> String? = ::defaultMovementAssetId
    ): ViewModelProvider.Factory {
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
            registerMovement = registerUC,
            editMovement = editUC,
            deleteMovement = deleteUC,
            swapMovement = swapUC,
            moveBetweenWallets = moveUC,
            portfolioIdProvider = portfolioIdProvider,
            walletIdResolver = walletIdResolver,
            assetIdResolver = assetIdResolver
        )
    }
}
val LocalAppDeps = staticCompositionLocalOf<AppDeps> {
    error("AppDeps not provided. Provide it from MainActivity using CompositionLocalProvider.")
}
