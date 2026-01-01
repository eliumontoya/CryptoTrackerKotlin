package info.eliumontoyasadec.cryptotracker.room.dao

import androidx.room.*
import info.eliumontoyasadec.cryptotracker.room.entities.CryptoEntity

@Dao
interface CryptoDao {

    @Query("DELETE FROM cryptos")
    suspend fun deleteAll()

    @Query("SELECT COUNT(*) FROM cryptos")
    suspend fun countAll(): Int
    @Query("SELECT * FROM cryptos ORDER BY name ASC")
    suspend fun getAll(): List<CryptoEntity>

    @Query("SELECT * FROM cryptos WHERE symbol = :symbol LIMIT 1")
    suspend fun getBySymbol(symbol: String): CryptoEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(items: List<CryptoEntity>)


    @Upsert
    suspend fun upsertOne(item: CryptoEntity)

    @Query("DELETE FROM cryptos WHERE symbol = :symbol")
    suspend fun deleteBySymbol(symbol: String): Int

}