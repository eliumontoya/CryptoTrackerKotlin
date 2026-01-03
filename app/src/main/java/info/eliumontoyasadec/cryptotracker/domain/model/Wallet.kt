package info.eliumontoyasadec.cryptotracker.domain.model

data class Wallet(
    val walletId: Long,
    val portfolioId: Long,
    val name: String,
    val description: String?,
    val isMain: Boolean
) {
    constructor(
        walletId: Long,
        portfolioId: Long,
        name: String, isMain: Boolean
    ) : this(walletId, portfolioId, name, "", isMain)


}