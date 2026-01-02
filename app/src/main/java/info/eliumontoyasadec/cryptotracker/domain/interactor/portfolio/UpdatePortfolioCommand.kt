package info.eliumontoyasadec.cryptotracker.domain.interactor.portfolio

data class UpdatePortfolioCommand(
    val id: Long,
    val nameRaw: String,
    val descriptionRaw: String?,
    val makeDefault: Boolean
)