package info.eliumontoyasadec.cryptotracker.room.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import info.eliumontoyasadec.cryptotracker.room.dao.CryptoDao
import info.eliumontoyasadec.cryptotracker.room.dao.FiatDao
import info.eliumontoyasadec.cryptotracker.room.dao.HoldingDao
import info.eliumontoyasadec.cryptotracker.room.dao.MovementDao
import info.eliumontoyasadec.cryptotracker.room.dao.PortfolioDao
import info.eliumontoyasadec.cryptotracker.room.dao.PortfolioSummaryDao
import info.eliumontoyasadec.cryptotracker.room.dao.WalletDao
import info.eliumontoyasadec.cryptotracker.room.queries.PortfolioQueriesDao
import info.eliumontoyasadec.cryptotracker.room.entities.CryptoEntity
import info.eliumontoyasadec.cryptotracker.room.entities.FiatEntity
import info.eliumontoyasadec.cryptotracker.room.entities.HoldingEntity
import info.eliumontoyasadec.cryptotracker.room.entities.PortfolioEntity
import info.eliumontoyasadec.cryptotracker.room.entities.MovementEntity
import info.eliumontoyasadec.cryptotracker.room.entities.WalletEntity

@Database(
    entities = [
        PortfolioEntity::class,
        WalletEntity::class,
        CryptoEntity::class,
        FiatEntity::class,
        HoldingEntity::class,
        MovementEntity::class
    ],
    version = 2,
    exportSchema = true
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {

    abstract fun portfolioDao(): PortfolioDao
    abstract fun walletDao(): WalletDao
    abstract fun cryptoDao(): CryptoDao
    abstract fun fiatDao(): FiatDao
    abstract fun holdingDao(): HoldingDao
    abstract fun movementDao(): MovementDao
    abstract fun portfolioSummaryDao(): PortfolioSummaryDao

    // Read-only aggregated queries for UI
    abstract fun portfolioQueriesDao(): PortfolioQueriesDao

    companion object {
        private const val DB_NAME = "cryptotracker.db"

        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    DB_NAME
                )
                    // En MVP está bien. En serio: antes de producción, migraciones.
                    .fallbackToDestructiveMigration()
                    .build()

                INSTANCE = instance
                instance
            }
        }
    }
}