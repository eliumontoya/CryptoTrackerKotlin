package info.eliumontoyasadec.cryptotracker.ui.screens.movements



fun defaultMovementAssetId(filter: CryptoFilter): String? =
    if (filter == CryptoFilter.ALL) null else filter.label