package info.eliumontoyasadec.cryptotracker.room.entities
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "portfolios")
data class PortfolioEntity(
    @PrimaryKey(autoGenerate = true)
    val portfolioId: Long = 0,
    val name: String,
    val description: String?,
    val isDefault: Boolean = false
)