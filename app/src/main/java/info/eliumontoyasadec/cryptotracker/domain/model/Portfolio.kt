package info.eliumontoyasadec.cryptotracker.domain.model

data class Portfolio (val portfolioId: Long = 0,
                 val name: String,
                 val description: String?,
                 val isDefault: Boolean = false)