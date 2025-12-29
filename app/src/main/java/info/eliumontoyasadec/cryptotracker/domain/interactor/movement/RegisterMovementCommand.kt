package info.eliumontoyasadec.cryptotracker.domain.interactor.movement

import info.eliumontoyasadec.cryptotracker.domain.model.MovementType

data class RegisterMovementCommand(
    val portfolioId: Long,
    val walletId: Long,
    val assetId: String,
    val type: MovementType,
    val quantity: Double,
    val price: Double? = null,
    val feeQuantity: Double? = null,
    val timestamp: Long,
    val notes: String = ""
)