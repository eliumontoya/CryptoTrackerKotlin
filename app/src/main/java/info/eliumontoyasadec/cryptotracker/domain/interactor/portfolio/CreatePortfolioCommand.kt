package info.eliumontoyasadec.cryptotracker.domain.interactor.portfolio

data class CreatePortfolioCommand(
    val nameRaw: String,
    val descriptionRaw: String?,
    val makeDefault: Boolean
)