package info.eliumontoyasadec.cryptotracker.data.seed

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import info.eliumontoyasadec.cryptotracker.domain.model.MovementType
import info.eliumontoyasadec.cryptotracker.room.db.AppDatabase
import info.eliumontoyasadec.cryptotracker.room.entities.CryptoEntity
import info.eliumontoyasadec.cryptotracker.room.entities.FiatEntity
import info.eliumontoyasadec.cryptotracker.room.entities.HoldingEntity
import info.eliumontoyasadec.cryptotracker.room.entities.MovementEntity
import info.eliumontoyasadec.cryptotracker.room.entities.PortfolioEntity
import info.eliumontoyasadec.cryptotracker.room.entities.WalletEntity
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class DatabaseWiperTest {

    private lateinit var db: AppDatabase
    private lateinit var wiper: DatabaseWiper

    @Before
    fun setUp() {
        val ctx = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(ctx, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()

        wiper = DatabaseWiper(db)
    }

    @After
    fun tearDown() {
        db.close()
    }

    @Test
    fun wipe_all_clears_all_tables_and_returns_counts() = runBlocking {
        val seeded = seedAll()

        // sanity: todo existe antes
        assertTrue(db.portfolioDao().countAll() > 0)
        assertTrue(db.walletDao().countAll() > 0)
        assertTrue(db.cryptoDao().countAll() > 0)
        assertTrue(db.fiatDao().countAll() > 0)
        assertTrue(db.movementDao().countAll() > 0)
        assertTrue(db.holdingDao().countAll() > 0)

        val res = wiper.wipe(
            DeleteRequest(
                all = true,
                wallets = false,
                cryptos = false,
                fiat = false,
                movements = false,
                holdings = false,
                portfolio = false
            )
        )

        // 1) BD queda vacía
        assertEquals(0, db.portfolioDao().countAll())
        assertEquals(0, db.walletDao().countAll())
        assertEquals(0, db.cryptoDao().countAll())
        assertEquals(0, db.fiatDao().countAll())
        assertEquals(0, db.movementDao().countAll())
        assertEquals(0, db.holdingDao().countAll())

        // 2) Contadores consistentes (si tu DeleteResult ya trae ints)
        // Ajusta los nombres si en tu proyecto se llaman distinto.
        assertTrue(res.deletedAllTables)

        // Si tu DeleteResult ya incluye contadores:
        // - walletsDeletedCount, cryptosDeletedCount, fiatDeletedCount, movementsDeletedCount, holdingsDeletedCount, portfoliosDeletedCount
        // valida contra lo que insertamos
        assertEquals(seeded.portfolios, res.portfoliosDeletedCount)
        assertEquals(seeded.wallets, res.walletsDeletedCount)
        assertEquals(seeded.cryptos, res.cryptosDeletedCount)
        assertEquals(seeded.fiat, res.fiatDeletedCount)
        assertEquals(seeded.movements, res.movementsDeletedCount)
        assertEquals(seeded.holdings, res.holdingsDeletedCount)
    }

    @Test
    fun wipe_deleting_cryptos_without_deleting_dependents_throws_fk_exception() = runBlocking {
        seedAll() // incluye movements/holdings apuntando a BTC/ETH

        try {
            wiper.wipe(
                DeleteRequest(
                    all = false,
                    wallets = false,
                    cryptos = true,
                    fiat = false,
                    movements = false,
                    holdings = false,
                    portfolio = false
                )
            )
            org.junit.Assert.fail("Expected SQLiteConstraintException due to FK constraint, but wipe completed.")
        } catch (t: Throwable) {
            // Puede llegar como SQLiteConstraintException directo o envuelto,
            // así que buscamos en la cadena de causas.
            val msg = (t.message ?: "") + " " + (t.cause?.message ?: "")
            val isFk = t is android.database.sqlite.SQLiteConstraintException ||
                    t.cause is android.database.sqlite.SQLiteConstraintException ||
                    msg.contains("FOREIGN KEY", ignoreCase = true)

            assertTrue(
                "Expected FK constraint failure, but got: ${t::class.java.simpleName} $msg",
                isFk
            )
        }

        // Aseguramos que no borró nada (o al menos que siguen existiendo dependencias)
        assertTrue(db.movementDao().countAll() > 0)
        assertTrue(db.holdingDao().countAll() > 0)
        assertTrue(db.cryptoDao().countAll() > 0)
    }

    @Test
    fun wipe_deleting_cryptos_with_dependents_succeeds() = runBlocking {
        seedAll()

        val res = wiper.wipe(
            DeleteRequest(
                all = false,
                wallets = false,
                cryptos = true,
                fiat = false,
                movements = true,
                holdings = true,
                portfolio = false
            )
        )

        // Debe borrar dependencias primero o como parte del wipe
        assertEquals(0, db.holdingDao().countAll())
        assertEquals(0, db.movementDao().countAll())
        assertEquals(0, db.cryptoDao().countAll())

        // Lo demás queda
        assertTrue(db.walletDao().countAll() > 0)
        assertTrue(db.portfolioDao().countAll() > 0)

        // Si tu DeleteResult tiene flags/contadores, valida aquí:
        // assertTrue(res.deletedCryptos)
        // assertTrue(res.deletedHoldings)
        // assertTrue(res.deletedMovements)
    }

    // -------------------------
    // Helpers
    // -------------------------

    private data class SeedCounts(
        val portfolios: Int,
        val wallets: Int,
        val cryptos: Int,
        val fiat: Int,
        val movements: Int,
        val holdings: Int
    )

    private suspend fun seedAll(): SeedCounts {
        // Portfolio
        val portfolioId = db.portfolioDao().insert(
            PortfolioEntity(
                name = "Default",
                description = "Seed portfolio",
                isDefault = true
            )
        )

        // Wallet
        val walletId = db.walletDao().insert(
            WalletEntity(
                portfolioId = portfolioId,
                name = "Main wallet",
                description = "Seed wallet",
                isMain = true
            )
        )

        // Cryptos
        val cryptos = listOf(
            CryptoEntity(
                symbol = "BTC",
                name = "Bitcoin",
                coingeckoId = "bitcoin",
                isActive = true
            ),
            CryptoEntity(
                symbol = "ETH",
                name = "Ethereum",
                coingeckoId = "ethereum",
                isActive = true
            ),
            CryptoEntity(symbol = "SOL", name = "Solana", coingeckoId = "solana", isActive = true),
            CryptoEntity(symbol = "XRP", name = "XRP", coingeckoId = "ripple", isActive = true),
        )
        db.cryptoDao().upsertAll(cryptos)

        // Fiat
        val fiat = listOf(
            FiatEntity(code = "USD", name = "US Dollar", symbol = "$"),
            FiatEntity(code = "MXN", name = "Peso Mexicano", symbol = "$"),
            FiatEntity(code = "EUR", name = "Euro", symbol = "€"),
            FiatEntity(code = "GBP", name = "Pound Sterling", symbol = "£"),
        )
        db.fiatDao().upsertAll(fiat)

        // Movements (requiere que exista assetId en cryptos)
        val now = System.currentTimeMillis()
        val movements = listOf(
            MovementEntity(
                id = 1L,
                portfolioId = portfolioId,
                walletId = walletId,
                assetId = "BTC",
                type = MovementType.BUY,
                quantity = 0.01,
                price = 50000.0,
                feeQuantity = 0.0,
                timestamp = now,
                notes = "seed",
                groupId = null
            ),
            MovementEntity(
                id = 2L,
                portfolioId = portfolioId,
                walletId = walletId,
                assetId = "ETH",
                type = MovementType.BUY,
                quantity = 0.2,
                price = 2500.0,
                feeQuantity = 0.0,
                timestamp = now + 1,
                notes = "seed",
                groupId = null
            )
        )
        movements.forEach { db.movementDao().upsert(it) }

        // Holdings
        val holdings = listOf(
            HoldingEntity(
                id = "${portfolioId}|${walletId}|BTC",
                portfolioId = portfolioId,
                walletId = walletId,
                assetId = "BTC",
                quantity = 0.01,
                updatedAt = now
            ),
            HoldingEntity(
                id = "${portfolioId}|${walletId}|ETH",
                portfolioId = portfolioId,
                walletId = walletId,
                assetId = "ETH",
                quantity = 0.2,
                updatedAt = now
            )
        )
        holdings.forEach { db.holdingDao().upsert(it) }

        return SeedCounts(
            portfolios = 1,
            wallets = 1,
            cryptos = cryptos.size,
            fiat = fiat.size,
            movements = movements.size,
            holdings = holdings.size
        )
    }
}