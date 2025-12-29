package info.eliumontoyasadec.cryptotracker.room.queries

import androidx.room.Dao
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface PortfolioQueriesDao {

    @Query("""
        SELECT
            w.portfolioId               AS portfolioId,
            h.assetId                   AS assetId,
            IFNULL(SUM(h.quantity), 0)  AS quantity,
            0.0                         AS costUsd,
            0.0                         AS realizedUsd,
            0.0                         AS pnlUsd,
            MAX(h.updatedAt)            AS updatedAt
        FROM holdings h
        INNER JOIN wallets w ON w.walletId = h.walletId
        WHERE w.portfolioId = :portfolioId
        GROUP BY w.portfolioId, h.assetId
        ORDER BY h.assetId ASC
    """)
    fun portfolioByCryptos(portfolioId: Long): Flow<List<PortfolioByCryptoRow>>

    @Query("""
        SELECT
            :portfolioId                          AS portfolioId,
            0.0                                   AS investedUsd,
            0.0                                   AS realizedUsd,
            MAX(h.updatedAt)                      AS updatedAt
        FROM holdings h
        INNER JOIN wallets w ON w.walletId = h.walletId
        WHERE w.portfolioId = :portfolioId
    """)
    fun portfolioSummary(portfolioId: Long): Flow<PortfolioSummaryRow>
}

/** Room-only projection for “Holdings por Crypto” */
data class PortfolioByCryptoRow(
    val portfolioId: Long,
    val assetId: String,
    val quantity: Double,
    val costUsd: Double,
    val realizedUsd: Double,
    val pnlUsd: Double,
    val updatedAt: Long?
)

/** Room-only projection for cards de resumen */
data class PortfolioSummaryRow(
    val portfolioId: Long,
    val investedUsd: Double,
    val realizedUsd: Double,
    val updatedAt: Long?
)