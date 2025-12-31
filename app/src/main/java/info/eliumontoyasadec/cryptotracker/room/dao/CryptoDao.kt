package info.eliumontoyasadec.cryptotracker.room.dao

import androidx.room.*
import info.eliumontoyasadec.cryptotracker.room.entities.CryptoEntity

@Dao
interface CryptoDao {
    @Query("SELECT COUNT(*) FROM portfolios") suspend fun countAll(): Int
    @Query("SELECT * FROM cryptos ORDER BY name ASC")
    suspend fun getAll(): List<CryptoEntity>

    @Query("SELECT * FROM cryptos WHERE symbol = :symbol LIMIT 1")
    suspend fun getBySymbol(symbol: String): CryptoEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(items: List<CryptoEntity>)
}