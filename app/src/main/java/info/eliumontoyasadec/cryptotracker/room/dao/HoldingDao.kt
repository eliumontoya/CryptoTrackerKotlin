package info.eliumontoyasadec.cryptotracker.room.dao

import androidx.room.*
import info.eliumontoyasadec.cryptotracker.room.entities.HoldingEntity

@Dao
interface HoldingDao {

    @Query("""
        SELECT * FROM holdings
        WHERE walletId = :walletId
        ORDER BY assetId ASC
    """)
    suspend fun getByWallet(walletId: Long): List<HoldingEntity>

    // ---- Queries for portfolio / wallet summaries (read-only) ----

    /**
     * Aggregated holdings by crypto for a given wallet.
     * Prices are not joined yet; currentPriceUsd will be provided later by a prices table.
     */
    @Query("""
        SELECT
            h.walletId            AS walletId,
            h.assetId        AS cryptoSymbol,
            SUM(h.quantity)       AS quantity,
            SUM(h.costUsd)        AS costUsd,
            SUM(h.realizedSalesUsd) AS realizedSalesUsd,
            SUM(h.realizedPnlUsd)   AS realizedPnlUsd
        FROM holdings h
        WHERE h.walletId = :walletId
        GROUP BY h.assetId
        ORDER BY h.assetId ASC
    """)
    suspend fun aggregateByCryptoForWallet(walletId: Long): List<WalletByCryptoRow>

    /**
     * Totals for a wallet derived from holdings.
     */
    @Query("""
        SELECT
            :walletId             AS walletId,
            IFNULL(SUM(h.costUsd), 0)           AS investedUsd,
            IFNULL(SUM(h.realizedSalesUsd), 0) AS realizedSalesUsd,
            IFNULL(SUM(h.realizedPnlUsd), 0)   AS realizedPnlUsd,
            MAX(h.updatedAt)      AS updatedAt
        FROM holdings h
        WHERE h.walletId = :walletId
    """)
    suspend fun walletSummary(walletId: Long): WalletSummaryRow

    @Query("""
        SELECT * FROM holdings
        WHERE walletId = :walletId AND assetId = :symbol
        LIMIT 1
    """)
    suspend fun find(walletId: Long, symbol: String): HoldingEntity?

    @Insert
    suspend fun insert(holding: HoldingEntity): Long

    @Query("""
        UPDATE holdings
        SET quantity = quantity + :delta,
            updatedAt = :updatedAt
        WHERE id = :holdingId
    """)
    suspend fun applyDelta(holdingId: Long, delta: Double, updatedAt: Long)

    @Query("DELETE FROM holdings WHERE walletId = :walletId")
    suspend fun deleteByWallet(walletId: Long)

    @Query("DELETE FROM holdings")
    suspend fun deleteAll()


    @Query(
        """
        SELECT * FROM holdings
        WHERE portfolioId = :portfolioId
          AND walletId = :walletId
          AND assetId = :assetId
        LIMIT 1
        """
    )
    suspend fun findByPortfolioWalletAsset(
        portfolioId: Long,
        walletId: Long,
        assetId: String
    ): HoldingEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: HoldingEntity)

    @Query(
        """
        SELECT * FROM holdings
        WHERE portfolioId = :portfolioId
          AND walletId = :walletId
        ORDER BY assetId ASC
        """
    )
    suspend fun listByWallet(
        portfolioId: Long,
        walletId: Long
    ): List<HoldingEntity>

    @Query(
        """
    SELECT * FROM holdings
    WHERE walletId = :walletId
      AND assetId = :assetId
    LIMIT 1
    """
    )
    suspend fun findByWalletAsset(walletId: Long, assetId: String): HoldingEntity?
}

 /**
  * Room projection for aggregated holdings by crypto (wallet scope).
  */
 data class WalletByCryptoRow(
     val walletId: Long,
     val cryptoSymbol: String,
     val quantity: Double,
     val costUsd: Double,
     val realizedSalesUsd: Double,
     val realizedPnlUsd: Double
 )

 /**
  * Room projection for wallet totals.
  */
 data class WalletSummaryRow(
     val walletId: Long,
     val investedUsd: Double,
     val realizedSalesUsd: Double,
     val realizedPnlUsd: Double,
     val updatedAt: Long?
 )