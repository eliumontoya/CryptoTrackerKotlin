package info.eliumontoyasadec.cryptotracker.room.queries

import androidx.room.Dao
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface PortfolioQueriesDao {

    @Query("""
        SELECT
            w.portfolioId            AS portfolioId,
            h.assetId           AS cryptoSymbol,
            IFNULL(SUM(h.quantity), 0)          AS quantity,
            IFNULL(SUM(h.costUsd), 0)           AS costUsd,
            IFNULL(SUM(h.realizedSalesUsd), 0)  AS realizedSalesUsd,
            IFNULL(SUM(h.realizedPnlUsd), 0)    AS realizedPnlUsd
        FROM holdings h
        INNER JOIN wallets w ON w.walletId = h.walletId
        WHERE w.portfolioId = :portfolioId
        GROUP BY h.assetId
        ORDER BY h.assetId ASC
    """)
    fun portfolioByCryptos(portfolioId: Long): Flow<List<PortfolioByCryptoRow>>

    @Query("""
        SELECT
            :portfolioId                          AS portfolioId,
            IFNULL(SUM(h.costUsd), 0)             AS investedUsd,
            IFNULL(SUM(h.realizedSalesUsd), 0)    AS realizedSalesUsd,
            IFNULL(SUM(h.realizedPnlUsd), 0)      AS realizedPnlUsd,
            MAX(h.updatedAt)                      AS updatedAt
        FROM holdings h
        INNER JOIN wallets w ON w.walletId = h.walletId
        WHERE w.portfolioId = :portfolioId
    """)
    fun portfolioSummary(portfolioId: Long): Flow<PortfolioSummaryRow>
}

/** Room-only projection for the table “Portafolio por Cryptos”. */
data class PortfolioByCryptoRow(
    val portfolioId: Long,
    val cryptoSymbol: String,
    val quantity: Double,
    val costUsd: Double,
    val realizedSalesUsd: Double,
    val realizedPnlUsd: Double
)

/** Room-only projection for the portfolio cards. */
data class PortfolioSummaryRow(
    val portfolioId: Long,
    val investedUsd: Double,
    val realizedSalesUsd: Double,
    val realizedPnlUsd: Double,
    val updatedAt: Long?
)