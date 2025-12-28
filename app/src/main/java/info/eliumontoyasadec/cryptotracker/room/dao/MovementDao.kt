package info.eliumontoyasadec.cryptotracker.room.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import info.eliumontoyasadec.cryptotracker.room.entities.MovementEntity

@Dao
interface MovementDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: MovementEntity)

    @Query("SELECT * FROM movements WHERE id = :id LIMIT 1")
    suspend fun findById(id: String): MovementEntity?

    @Update
    suspend fun update(entity: MovementEntity)

    @Query("DELETE FROM movements WHERE id = :id")
    suspend fun deleteById(id: String)

    @Query(
        """
        SELECT * FROM movements
        WHERE portfolioId = :portfolioId
          AND walletId = :walletId
        ORDER BY timestamp DESC
        """
    )
    suspend fun listByWallet(
        portfolioId: String,
        walletId: String
    ): List<MovementEntity>
}