package info.eliumontoyasadec.cryptotracker

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import info.eliumontoyasadec.cryptotracker.data.queries.RoomPortfolioQueries
import info.eliumontoyasadec.cryptotracker.data.seed.CatalogSeeder
import info.eliumontoyasadec.cryptotracker.data.seed.DatabaseWiper
import info.eliumontoyasadec.cryptotracker.room.db.AppDatabase
import info.eliumontoyasadec.cryptotracker.room.repositories.CryptoRepositoryRoom
import info.eliumontoyasadec.cryptotracker.room.repositories.PortfolioRepositoryRoom
import info.eliumontoyasadec.cryptotracker.room.repositories.WalletRepositoryRoom
import info.eliumontoyasadec.cryptotracker.ui.shell.AppDeps
import info.eliumontoyasadec.cryptotracker.ui.shell.AppShell
import info.eliumontoyasadec.cryptotracker.ui.shell.LocalAppDeps

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            val appDeps = remember {
                val db = AppDatabase.getInstance(applicationContext)
                val portfolioQueriesDao = db.portfolioQueriesDao()


                AppDeps(
                portfolioQueries = RoomPortfolioQueries(portfolioQueriesDao),
                    catalogSeeder = CatalogSeeder(db),
                    databaseWiper = DatabaseWiper(db),
                    portfolioRepository = PortfolioRepositoryRoom(db.portfolioDao()),
                    walletRepository = WalletRepositoryRoom(db.walletDao()),
                    cryptoRepository = CryptoRepositoryRoom(db.cryptoDao())


                )
            }

            CompositionLocalProvider(LocalAppDeps provides appDeps) {
                AppShell()
            }
        }
    }
}