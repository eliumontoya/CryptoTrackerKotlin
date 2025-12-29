package info.eliumontoyasadec.cryptotracker.domain.model

data class Fiat(
    val code: String,   // MXN, USD
    val name: String,
    val symbol: String  // $, €
)