package info.eliumontoyasadec.cryptotracker.ui.screens.movements

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import info.eliumontoyasadec.cryptotracker.domain.interactor.movement.DeleteMovementUseCase
import info.eliumontoyasadec.cryptotracker.domain.interactor.movement.EditMovementUseCase
import info.eliumontoyasadec.cryptotracker.domain.interactor.movement.LoadMovementsUseCase
import info.eliumontoyasadec.cryptotracker.domain.interactor.movement.MoveBetweenWalletsUseCase
import info.eliumontoyasadec.cryptotracker.domain.interactor.movement.RegisterMovementUseCase
import info.eliumontoyasadec.cryptotracker.domain.interactor.movement.SwapMovementUseCase

class MovementsViewModelFactory(
    private val mode: MovementMode,
    private val loadMovements: LoadMovementsUseCase? = null,
    private val registerMovement: RegisterMovementUseCase? = null,
    private val editMovement: EditMovementUseCase? = null,
    private val deleteMovement: DeleteMovementUseCase? = null,
    private val swapMovement: SwapMovementUseCase? = null,
    private val moveBetweenWallets: MoveBetweenWalletsUseCase? = null,
    private val portfolioIdProvider: () -> Long = { 1L },
    private val walletIdResolver: (WalletFilter) -> Long? = { null },
    private val assetIdResolver: (CryptoFilter) -> String? = { null }
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
                moveBetweenWallets = moveBetweenWallets,
                portfolioIdProvider = portfolioIdProvider,
                walletIdResolver = walletIdResolver,
                assetIdResolver = assetIdResolver
            ) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}