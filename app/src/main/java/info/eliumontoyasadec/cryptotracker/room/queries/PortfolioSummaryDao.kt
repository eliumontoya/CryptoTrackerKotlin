package info.eliumontoyasadec.cryptotracker.room.queries

import androidx.room.Dao
import androidx.room.Query
import info.eliumontoyasadec.cryptotracker.room.queries.rows.PortfolioTotalRow
import info.eliumontoyasadec.cryptotracker.room.queries.rows.PortfolioWalletTotalRow
import info.eliumontoyasadec.cryptotracker.room.queries.rows.WalletHoldingRow

@Dao
interface PortfolioSummaryDao {

    /**
     * Resumen general del portafolio:
     * - # wallets
     * - # cryptos distintos (en holdings)
     * - última fecha de actualización (máxima updatedAt)
     */
    @Query("""
        SELECT 
            p.portfolioId AS portfolioId,
            p.name AS portfolioName,
            COUNT(DISTINCT w.walletId) AS totalWallets,
            COUNT(DISTINCT h.assetId) AS totalDistinctCryptos,
            MAX(h.updatedAt) AS lastUpdatedAt
        FROM portfolios p
        LEFT JOIN wallets w ON w.portfolioId = p.portfolioId
        LEFT JOIN holdings h ON h.walletId = w.walletId
        WHERE p.portfolioId = :portfolioId
        GROUP BY p.portfolioId, p.name
    """)
    suspend fun getPortfolioTotal(portfolioId: Long): PortfolioTotalRow?

    /**
     * Totales por wallet dentro de un portafolio:
     * - # cryptos distintos por wallet
     * - última actualización por wallet
     */
    @Query("""
        SELECT
            w.portfolioId AS portfolioId,
            w.walletId AS walletId,
            w.name AS walletName,
            COUNT(DISTINCT h.assetId) AS totalDistinctCryptos,
            MAX(h.updatedAt) AS lastUpdatedAt
        FROM wallets w
        LEFT JOIN holdings h ON h.walletId = w.walletId
        WHERE w.portfolioId = :portfolioId
        GROUP BY w.portfolioId, w.walletId, w.name
        ORDER BY w.isMain DESC, w.name ASC
    """)
    suspend fun getWalletTotalsByPortfolio(portfolioId: Long): List<PortfolioWalletTotalRow>

    /**
     * Detalle de holdings por wallet (para pantalla "Desglose por cartera"):
     * Incluye nombre de crypto desde la tabla cryptos.
     */
    @Query("""
        SELECT
            w.walletId AS walletId,
            w.name AS walletName,
            h.assetId AS assetId,
            c.name AS cryptoName,
            h.quantity AS quantity,
            h.updatedAt AS updatedAt
        FROM wallets w
        INNER JOIN holdings h ON h.walletId = w.walletId
        LEFT JOIN cryptos c ON c.symbol = h.assetId
        WHERE w.walletId = :walletId
        ORDER BY h.assetId ASC
    """)
    suspend fun getHoldingsByWallet(walletId: Long): List<WalletHoldingRow>

    /**
     * Detalle completo por portafolio (para "Desglose por cryptos" si lo quieres):
     * lista todos los holdings de todas las wallets del portafolio.
     */
    @Query("""
        SELECT
            w.walletId AS walletId,
            w.name AS walletName,
            h.assetId AS assetId,
            c.name AS cryptoName,
            h.quantity AS quantity,
            h.updatedAt AS updatedAt
        FROM wallets w
        INNER JOIN holdings h ON h.walletId = w.walletId
        LEFT JOIN cryptos c ON c.symbol = h.assetId
        WHERE w.portfolioId = :portfolioId
        ORDER BY w.isMain DESC, w.name ASC, h.assetId ASC
    """)
    suspend fun getHoldingsByPortfolio(portfolioId: Long): List<WalletHoldingRow>
}