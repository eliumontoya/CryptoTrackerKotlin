package info.eliumontoyasadec.cryptotracker.ui.portfolio

import java.text.NumberFormat
import java.util.Locale

private val usdFormat = NumberFormat.getCurrencyInstance(Locale.US)
private val qtyFormat = NumberFormat.getNumberInstance(Locale.US).apply {
    minimumFractionDigits = 0
    maximumFractionDigits = 6
}

fun formatUsd(value: Double): String = usdFormat.format(value)
fun formatQty(value: Double): String = qtyFormat.format(value)