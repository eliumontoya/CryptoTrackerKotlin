package info.eliumontoyasadec.cryptotracker.data.seed

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import info.eliumontoyasadec.cryptotracker.room.db.AppDatabase
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class CatalogSeederTest {

    private lateinit var db: AppDatabase
    private lateinit var seeder: CatalogSeeder

    @Before
    fun setup() {
        val ctx: Context = ApplicationProvider.getApplicationContext()
        db = Room.inMemoryDatabaseBuilder(ctx, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()

        seeder = CatalogSeeder(db)
    }

    @After
    fun tearDown() {
        db.close()
    }

    @Test
    fun seed_inserts_expected_catalogs() = runBlocking {
        // Estado inicial
        val beforeCryptos = db.cryptoDao().countAll()
        val beforeFiat = db.fiatDao().countAll()

        val res = seeder.seed(
            SeedRequest(
                wallets = true,
                cryptos = true,
                fiat = true,
                syncManual = true
            )
        )

        // Estado final
        val afterCryptos = db.cryptoDao().getAll().size
        val afterFiat = db.fiatDao().getAll().size

        // Verifica BD (lo más importante)
        assertEquals(InitialCatalogSeed.cryptos.size, afterCryptos)
        assertEquals(InitialCatalogSeed.fiat.size, afterFiat)

        // Verifica consistencia del resultado (delta real)
        assertEquals(afterCryptos - beforeCryptos, res.cryptosUpserted)
        assertEquals(afterFiat - beforeFiat, res.fiatUpserted)
    }

    @Test
    fun seed_twice_is_idempotent_for_crypto_and_fiat() = runBlocking {
        seeder.seed(SeedRequest(wallets = false, cryptos = true, fiat = true, syncManual = false))
        seeder.seed(SeedRequest(wallets = false, cryptos = true, fiat = true, syncManual = false))

        val cryptos = db.cryptoDao().getAll()
        val fiat = db.fiatDao().getAll()

        assertEquals(InitialCatalogSeed.cryptos.size, cryptos.size)
        assertEquals(InitialCatalogSeed.fiat.size, fiat.size)
    }
}