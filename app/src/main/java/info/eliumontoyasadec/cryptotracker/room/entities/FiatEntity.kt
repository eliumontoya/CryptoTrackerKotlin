package info.eliumontoyasadec.cryptotracker.room.entities
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "fiat")
data class FiatEntity(
    @PrimaryKey
    val code: String,      // MXN, USD
    val name: String,
    val symbol: String     // $, €
)