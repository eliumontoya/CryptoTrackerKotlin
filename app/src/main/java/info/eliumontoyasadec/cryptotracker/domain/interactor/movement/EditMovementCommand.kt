package info.eliumontoyasadec.cryptotracker.domain.interactor.movement

import info.eliumontoyasadec.cryptotracker.domain.model.MovementType

data class EditMovementCommand(
    val movementId: String,
    val newType: MovementType,
    val newQuantity: Double,
    val newPrice: Double? = null,
    val newFeeQuantity: Double? = null,
    val newTimestamp: Long,
    val newNotes: String? = null
)