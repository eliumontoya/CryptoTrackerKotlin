package info.eliumontoyasadec.cryptotracker.room.entities

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "holdings",
    foreignKeys = [
        ForeignKey(
            entity = WalletEntity::class,
            parentColumns = ["walletId"],
            childColumns = ["walletId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = CryptoEntity::class,
            parentColumns = ["symbol"],
            childColumns = ["assetId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index("walletId"),
        Index("assetId"),
        Index(value = ["portfolioId", "walletId", "assetId"], unique = true)
    ]
)
data class HoldingEntity(
    @PrimaryKey val id: String, // "$portfolioId|$walletId|$assetId"
    val portfolioId: Long,
    val walletId: Long,
    val assetId: String,
    // Net position
    val quantity: Double,

    // Cost basis still allocated to the remaining quantity (USD)
    val costUsd: Double = 0.0,

    // Realized totals from sells (USD). Useful for portfolio summaries.
    val realizedSalesUsd: Double = 0.0,
    val realizedPnlUsd: Double = 0.0,

    val updatedAt: Long
)