package info.eliumontoyasadec.cryptotracker.room.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import info.eliumontoyasadec.cryptotracker.room.entities.HoldingEntity

@Dao
interface HoldingDao {

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
        portfolioId: String,
        walletId: String,
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
        portfolioId: String,
        walletId: String
    ): List<HoldingEntity>
}