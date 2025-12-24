package info.eliumontoyasadec.cryptotracker.room.dao

import androidx.room.*
import info.eliumontoyasadec.cryptotracker.room.entities.PortfolioEntity

@Dao
interface PortfolioDao {

    @Query("SELECT * FROM portfolios ORDER BY isDefault DESC, name ASC")
    suspend fun getAll(): List<PortfolioEntity>

    @Query("SELECT * FROM portfolios WHERE portfolioId = :id LIMIT 1")
    suspend fun getById(id: Long): PortfolioEntity?

    @Query("SELECT * FROM portfolios WHERE isDefault = 1 LIMIT 1")
    suspend fun getDefault(): PortfolioEntity?

    @Insert
    suspend fun insert(portfolio: PortfolioEntity): Long

    @Update
    suspend fun update(portfolio: PortfolioEntity)

    @Delete
    suspend fun delete(portfolio: PortfolioEntity)
}