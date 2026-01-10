package info.eliumontoyasadec.cryptotracker.domain.interactor.movement

import info.eliumontoyasadec.cryptotracker.domain.model.Movement

data class LoadMovementsResult(
    val items: List<Movement>
)