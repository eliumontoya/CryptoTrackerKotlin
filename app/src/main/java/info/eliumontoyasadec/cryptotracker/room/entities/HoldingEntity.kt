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
    val portfolioId: String,
    val walletId: String,
    val assetId: String,
    val quantity: Double,
    val updatedAt: Long
)