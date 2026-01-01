package info.eliumontoyasadec.cryptotracker.room.dao

import androidx.room.*
import info.eliumontoyasadec.cryptotracker.room.entities.FiatEntity

@Dao
interface FiatDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(items: List<FiatEntity>)

    // NUEVO: upsert unitario (para Admin)
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(item: FiatEntity)

    @Query("SELECT * FROM fiat ORDER BY code ASC")
    suspend fun getAll(): List<FiatEntity>

    @Query("SELECT * FROM fiat WHERE code = :code LIMIT 1")
    suspend fun getByCode(code: String): FiatEntity?

    // NUEVO: delete selectivo (para Admin)
    @Query("DELETE FROM fiat WHERE code = :code")
    suspend fun deleteByCode(code: String): Int

    @Query("DELETE FROM fiat")
    suspend fun deleteAll(): Int

    @Query("SELECT COUNT(*) FROM fiat")
    suspend fun countAll(): Int
}