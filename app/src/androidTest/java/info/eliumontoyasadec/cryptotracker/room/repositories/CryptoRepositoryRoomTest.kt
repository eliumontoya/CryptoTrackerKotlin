package info.eliumontoyasadec.cryptotracker.room.repositories

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import info.eliumontoyasadec.cryptotracker.domain.model.Crypto
import info.eliumontoyasadec.cryptotracker.room.RoomTestSeed
import info.eliumontoyasadec.cryptotracker.room.db.AppDatabase
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class CryptoRepositoryRoomTest {

    private lateinit var db: AppDatabase
    private lateinit var repo: CryptoRepositoryRoom

    @Before
    fun setup() {
        db = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            AppDatabase::class.java
        ).allowMainThreadQueries().build()

        RoomTestSeed.enableForeignKeys(db.openHelper.writableDatabase)

        repo = CryptoRepositoryRoom(db.cryptoDao())
    }

    @After
    fun tearDown() = db.close()

    @Test
    fun getAll_is_sorted_by_name() = runTest {
        repo.upsertAll(
            listOf(
                Crypto(symbol = "ETH", name = "Ethereum", coingeckoId = null),
                Crypto(symbol = "BTC", name = "Bitcoin", coingeckoId = null),
            )
        )

        val items = repo.getAll()
        assertEquals(listOf("Bitcoin", "Ethereum"), items.map { it.name })
    }

    @Test
    fun upsert_updates_existing_by_symbol() = runTest {
        repo.upsertAll(listOf(Crypto(symbol = "BTC", name = "Bitcoin", coingeckoId = null)))

        repo.upsertAll(listOf(Crypto(symbol = "BTC", name = "Bitcoin (Updated)", coingeckoId = null)))

        val btc = repo.findBySymbol("BTC")
        assertEquals("Bitcoin (Updated)", btc?.name)
    }

    @Test
    fun upsertAll_getAll_ordering_findBySymbol_replace() = runTest {
        repo.upsertAll(
            listOf(
                Crypto(symbol = "btc", name = "Bitcoin", coingeckoId = "bitcoin", isActive = true),
                Crypto(symbol = "eth", name = "Ethereum", coingeckoId = "ethereum", isActive = true)
            )
        )

        val all = repo.getAll()
        assertEquals(2, all.size)
        // ORDER BY name ASC (lo impone el DAO)
        assertEquals("Bitcoin", all[0].name)
        assertEquals("Ethereum", all[1].name)

        val btc = repo.findBySymbol("btc")
        assertNotNull(btc)
        assertEquals("Bitcoin", btc!!.name)

        // replace
        repo.upsertAll(listOf(Crypto(symbol = "btc", name = "Bitcoin v2", coingeckoId = "bitcoin", isActive = true)))
        val btc2 = repo.findBySymbol("btc")
        assertEquals("Bitcoin v2", btc2!!.name)
    }

    @Test
    fun exists_returnsTrue_whenSymbolPresent() = runTest {
        repo.upsertAll(listOf(Crypto(symbol = "btc", name = "Bitcoin", coingeckoId = "bitcoin", isActive = true)))
        assertTrue(repo.exists("btc"))
        assertFalse(repo.exists("xrp"))
    }
}