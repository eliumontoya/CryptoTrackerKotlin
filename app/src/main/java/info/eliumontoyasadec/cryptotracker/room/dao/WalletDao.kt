package info.eliumontoyasadec.cryptotracker.room.dao

import androidx.room.*
import info.eliumontoyasadec.cryptotracker.room.entities.WalletEntity

@Dao
interface WalletDao {

    @Query("SELECT * FROM wallets WHERE portfolioId = :portfolioId ORDER BY isMain DESC, name ASC")
    suspend fun getByPortfolio(portfolioId: Long): List<WalletEntity>

    @Query("SELECT * FROM wallets WHERE walletId = :walletId LIMIT 1")
    suspend fun getById(walletId: Long): WalletEntity?

    @Insert
    suspend fun insert(wallet: WalletEntity): Long

    @Update
    suspend fun update(wallet: WalletEntity)

    @Delete
    suspend fun delete(wallet: WalletEntity)
}