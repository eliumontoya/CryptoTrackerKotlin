package info.eliumontoyasadec.cryptotracker.room.dao

import androidx.room.*
import info.eliumontoyasadec.cryptotracker.room.entities.FiatEntity

@Dao
interface FiatDao {
    @Query("SELECT COUNT(*) FROM portfolios") suspend fun countAll(): Int
    @Query("SELECT * FROM fiat ORDER BY code ASC")
    suspend fun getAll(): List<FiatEntity>

    @Query("SELECT * FROM fiat WHERE code = :code LIMIT 1")
    suspend fun getByCode(code: String): FiatEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(items: List<FiatEntity>)
}