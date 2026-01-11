package info.eliumontoyasadec.cryptotracker.ui.screens.movements

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import info.eliumontoyasadec.cryptotracker.domain.interactor.movement.DeleteMovementUseCase
import info.eliumontoyasadec.cryptotracker.domain.interactor.movement.EditMovementUseCase
import info.eliumontoyasadec.cryptotracker.domain.interactor.movement.LoadMovementsUseCase
import info.eliumontoyasadec.cryptotracker.domain.interactor.movement.MoveBetweenWalletsUseCase
import info.eliumontoyasadec.cryptotracker.domain.interactor.movement.RegisterMovementUseCase
import info.eliumontoyasadec.cryptotracker.domain.interactor.movement.SwapMovementUseCase
import info.eliumontoyasadec.cryptotracker.domain.repositories.PortfolioRepository
import info.eliumontoyasadec.cryptotracker.domain.repositories.WalletRepository

class MovementsViewModelFactory(
    private val mode: MovementMode,
    private val loadMovements: LoadMovementsUseCase,
    private val registerMovement: RegisterMovementUseCase? = null,
    private val editMovement: EditMovementUseCase? = null,
    private val deleteMovement: DeleteMovementUseCase? = null,
    private val swapMovement: SwapMovementUseCase? = null,
    private val moveBetweenWallets: MoveBetweenWalletsUseCase? = null,
    private val portfolioRepo: PortfolioRepository,
    private val walletRepo: WalletRepository,
    private val assetIdResolver: (CryptoFilter) -> String?
) : ViewModelProvider.Factory {

    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(MovementsViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return MovementsViewModel(
                mode = mode,
                loadMovements = loadMovements,
                registerMovement = registerMovement,
                editMovement = editMovement,
                deleteMovement = deleteMovement,
                swapMovement = swapMovement,
                 portfolioRepo = portfolioRepo,
                walletRepo = walletRepo,
                assetIdResolver = assetIdResolver
            ) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}