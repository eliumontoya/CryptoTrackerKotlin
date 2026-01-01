package info.eliumontoyasadec.cryptotracker.room.dao

import androidx.room.*
import info.eliumontoyasadec.cryptotracker.room.entities.WalletEntity

@Dao
interface WalletDao {

    @Query("SELECT * FROM wallets WHERE portfolioId = :portfolioId ORDER BY isMain DESC, name ASC")
    suspend fun getByPortfolio(portfolioId: Long): List<WalletEntity>

    @Query("UPDATE wallets SET name = :name WHERE walletId = :walletId")
    suspend fun updateName(walletId: Long, name: String): Int
    @Query("SELECT * FROM wallets WHERE walletId = :walletId LIMIT 1")
    suspend fun getById(walletId: Long): WalletEntity?

    @Insert
    suspend fun insert(wallet: WalletEntity): Long

    @Update
    suspend fun update(wallet: WalletEntity)

    @Delete
    suspend fun delete(wallet: WalletEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: WalletEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(items: List<WalletEntity>)

    @Query("DELETE FROM wallets")
    suspend fun deleteAll()

    @Query("SELECT COUNT(*) FROM wallets")
    suspend fun countAll(): Int


    @Query("UPDATE wallets SET isMain = 0 WHERE portfolioId = :portfolioId")
    suspend fun clearMainForPortfolio(portfolioId: Long): Int

    @Query("UPDATE wallets SET isMain = 1 WHERE walletId = :walletId")
    suspend fun markMain(walletId: Long): Int


    @Query("SELECT isMain FROM wallets WHERE walletId = :walletId")
    suspend fun isMain(walletId: Long): Boolean?

    @Query("SELECT portfolioId FROM wallets WHERE walletId = :walletId")
    suspend fun portfolioIdOf(walletId: Long): Long?

    @Query("DELETE FROM wallets WHERE walletId = :walletId")
    suspend fun deleteById(walletId: Long): Int



}