package info.eliumontoyasadec.cryptotracker.room.entities

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import info.eliumontoyasadec.cryptotracker.domain.model.MovementType
import java.time.Instant

@Entity(
    tableName = "movements",
    foreignKeys = [
        ForeignKey(
            entity = WalletEntity::class,
            parentColumns = ["walletId"],
            childColumns = ["walletId"],
            onDelete = ForeignKey.NO_ACTION
        ),
        ForeignKey(
            entity = CryptoEntity::class,
            parentColumns = ["symbol"],
            childColumns = ["assetId"],
            onDelete = ForeignKey.NO_ACTION
        )
    ],
    indices = [
        Index("walletId"),
        Index("assetId")
    ]
)
data class MovementEntity(
    @PrimaryKey
    val id: Long,

    val portfolioId: Long,
    val walletId: Long,
    val assetId: String,

    val type: MovementType,
    val quantity: Double,
    val price: Double?,      // opcional
    val feeQuantity: Double, // 0.0 si no aplica

    val timestamp: Long,
    val notes: String?,
    val groupId: Long? = null

)