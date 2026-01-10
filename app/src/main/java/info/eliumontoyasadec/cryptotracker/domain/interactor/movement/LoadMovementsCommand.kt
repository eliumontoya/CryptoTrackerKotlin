package info.eliumontoyasadec.cryptotracker.domain.interactor.movement

data class LoadMovementsCommand(
    val portfolioId: Long,
    val walletId: Long? = null,
    val assetId: String? = null
)