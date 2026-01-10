package info.eliumontoyasadec.cryptotracker.ui.screens.movements

import info.eliumontoyasadec.cryptotracker.domain.model.MovementType

fun MovementTypeUi.toDomain(): MovementType = when (this) {
    MovementTypeUi.BUY -> MovementType.BUY
    MovementTypeUi.SELL -> MovementType.SELL
    MovementTypeUi.DEPOSIT -> MovementType.DEPOSIT
    MovementTypeUi.WITHDRAW -> MovementType.WITHDRAW
    MovementTypeUi.TRANSFER_IN -> MovementType.TRANSFER_IN
    MovementTypeUi.TRANSFER_OUT -> MovementType.TRANSFER_OUT
    MovementTypeUi.FEE -> MovementType.FEE
}