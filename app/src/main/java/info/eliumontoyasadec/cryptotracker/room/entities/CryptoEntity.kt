package info.eliumontoyasadec.cryptotracker.room.entities
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "cryptos")
data class CryptoEntity(
    @PrimaryKey  val symbol: String,        // BTC, ETH
    val name: String,          // Bitcoin
    val coingeckoId: String?,  // para APIs externas
    val isActive: Boolean = true
)