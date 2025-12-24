package info.eliumontoyasadec.cryptotracker.room.dao

import androidx.room.*
import info.eliumontoyasadec.cryptotracker.room.entities.HoldingEntity

@Dao
interface HoldingDao {

    @Query("""
        SELECT * FROM holdings
        WHERE walletId = :walletId
        ORDER BY cryptoSymbol ASC
    """)
    suspend fun getByWallet(walletId: Long): List<HoldingEntity>

    @Query("""
        SELECT * FROM holdings
        WHERE walletId = :walletId AND cryptoSymbol = :symbol
        LIMIT 1
    """)
    suspend fun find(walletId: Long, symbol: String): HoldingEntity?

    @Insert
    suspend fun insert(holding: HoldingEntity): Long

    @Query("""
        UPDATE holdings
        SET quantity = quantity + :delta,
            updatedAt = :updatedAt
        WHERE holdingId = :holdingId
    """)
    suspend fun applyDelta(holdingId: Long, delta: Double, updatedAt: Long)

    @Query("DELETE FROM holdings WHERE walletId = :walletId")
    suspend fun deleteByWallet(walletId: Long)

    @Query("DELETE FROM holdings")
    suspend fun deleteAll()
}