package info.eliumontoyasadec.cryptotracker.ui.screens.movements
fun defaultMovementWalletId(filter: WalletFilter): Long? = when (filter) {
    WalletFilter.ALL -> null
    WalletFilter.METAMASK -> 1L
    WalletFilter.BYBIT -> 2L
    WalletFilter.PHANTOM -> 3L
}

fun defaultMovementAssetId(filter: CryptoFilter): String? = when (filter) {
    CryptoFilter.ALL -> null
    else -> filter.label
}