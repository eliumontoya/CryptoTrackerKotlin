package info.eliumontoyasadec.cryptotracker.room.dao

import androidx.room.*
import info.eliumontoyasadec.cryptotracker.room.entities.FiatEntity

@Dao
interface FiatDao {
     @Query("SELECT * FROM fiat ORDER BY code ASC")
    suspend fun getAll(): List<FiatEntity>

    @Query("SELECT * FROM fiat WHERE code = :code LIMIT 1")
    suspend fun getByCode(code: String): FiatEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(items: List<FiatEntity>)

    @Query("DELETE FROM fiat")
    suspend fun deleteAll()

    @Query("SELECT COUNT(*) FROM fiat")
    suspend fun countAll(): Int
}