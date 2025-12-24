package info.eliumontoyasadec.cryptotracker.room.entities
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "wallets",
    foreignKeys = [
        ForeignKey(
            entity = PortfolioEntity::class,
            parentColumns = ["portfolioId"],
            childColumns = ["portfolioId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("portfolioId")]
)
data class WalletEntity(
    @PrimaryKey(autoGenerate = true)
    val walletId: Long = 0,
    val portfolioId: Long,     // <-- NUEVO
    val name: String,
    val description: String?,
    val isMain: Boolean = false
)