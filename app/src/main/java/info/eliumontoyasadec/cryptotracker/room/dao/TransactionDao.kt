package info.eliumontoyasadec.cryptotracker.room.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import info.eliumontoyasadec.cryptotracker.room.entities.TransactionEntity

@Dao
interface TransactionDao {

    @Insert
    suspend fun insert(tx: TransactionEntity): Long

    @Query("""
        SELECT * FROM transactions
        WHERE walletId = :walletId
        ORDER BY timestamp DESC
    """)
    suspend fun getByWallet(walletId: Long): List<TransactionEntity>

    @Query("""
        SELECT walletId, cryptoSymbol,
               SUM(
                    CASE type
                        WHEN 'BUY' THEN quantity
                        WHEN 'TRANSFER_IN' THEN quantity
                        WHEN 'SELL' THEN -quantity
                        WHEN 'TRANSFER_OUT' THEN -quantity
                        WHEN 'ADJUSTMENT' THEN quantity
                        ELSE 0
                    END
               ) AS balance
        FROM transactions
        WHERE walletId = :walletId
        GROUP BY walletId, cryptoSymbol
    """)
    suspend fun computeBalancesByWallet(walletId: Long): List<WalletCryptoBalanceRow>

    @Query("""
        SELECT walletId, cryptoSymbol,
               SUM(
                    CASE type
                        WHEN 'BUY' THEN quantity
                        WHEN 'TRANSFER_IN' THEN quantity
                        WHEN 'SELL' THEN -quantity
                        WHEN 'TRANSFER_OUT' THEN -quantity
                        WHEN 'ADJUSTMENT' THEN quantity
                        ELSE 0
                    END
               ) AS balance
        FROM transactions
        GROUP BY walletId, cryptoSymbol
    """)
    suspend fun computeBalancesAll(): List<WalletCryptoBalanceRow>
}

data class WalletCryptoBalanceRow(
    val walletId: Long,
    val cryptoSymbol: String,
    val balance: Double
)