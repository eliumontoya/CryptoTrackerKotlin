package info.eliumontoyasadec.cryptotracker.room.entities

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import info.eliumontoyasadec.cryptotracker.domain.model.MovementType
import java.time.Instant

@Entity(
    tableName = "transactions",
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
            childColumns = ["cryptoSymbol"],
            onDelete = ForeignKey.NO_ACTION
        )
    ],
    indices = [
        Index("walletId"),
        Index("cryptoSymbol")
    ]
)
data class TransactionEntity(
    @PrimaryKey(autoGenerate = true)
    val transactionId: Long = 0,

    val walletId: Long,
    val cryptoSymbol: String,

    val type: MovementType,

    // Cantidad de crypto (ej. 0.01 BTC)
    val quantity: Double,

    // Para BUY/SELL (en transferencias puede ser null si quieres)
    val priceFiat: Double? = null,
    val fiatCode: String? = null,

    val timestamp: Long = Instant.now().toEpochMilli(),
    val notes: String? = null
)