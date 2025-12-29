package info.eliumontoyasadec.cryptotracker.room.repositories

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import info.eliumontoyasadec.cryptotracker.room.db.AppDatabase
import info.eliumontoyasadec.cryptotracker.room.entities.CryptoEntity
import info.eliumontoyasadec.cryptotracker.room.RoomTestSeed
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
    fun upsertAll_getAll_ordering_getBySymbol_replace() = runTest {
        repo.upsertAll(
            listOf(
                CryptoEntity(symbol = "btc", name = "Bitcoin", coingeckoId = "bitcoin", isActive = true),
                CryptoEntity(symbol = "eth", name = "Ethereum", coingeckoId = "ethereum", isActive = true)
            )
        )

        val all = repo.getAll()
        assertEquals(2, all.size)
        // ORDER BY name ASC
        assertEquals("Bitcoin", all[0].name)
        assertEquals("Ethereum", all[1].name)

        val btc = repo.getBySymbol("btc")
        assertNotNull(btc)
        assertEquals("Bitcoin", btc!!.name)

        // replace
        repo.upsertAll(listOf(CryptoEntity(symbol = "btc", name = "Bitcoin v2", coingeckoId = "bitcoin", isActive = true)))
        val btc2 = repo.getBySymbol("btc")
        assertEquals("Bitcoin v2", btc2!!.name)
    }
}