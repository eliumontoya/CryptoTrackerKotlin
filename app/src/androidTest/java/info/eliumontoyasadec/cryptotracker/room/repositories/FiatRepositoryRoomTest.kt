package info.eliumontoyasadec.cryptotracker.room.repositories

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import info.eliumontoyasadec.cryptotracker.domain.model.Fiat
import info.eliumontoyasadec.cryptotracker.room.RoomTestSeed
import info.eliumontoyasadec.cryptotracker.room.db.AppDatabase
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class FiatRepositoryRoomTest {

    private lateinit var db: AppDatabase
    private lateinit var repo: FiatRepositoryRoom

    @Before
    fun setup() {
        db = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            AppDatabase::class.java
        )
            .allowMainThreadQueries()
            .build()

        RoomTestSeed.enableForeignKeys(db.openHelper.writableDatabase)

        repo = FiatRepositoryRoom(db.fiatDao())
    }

    @After
    fun tearDown() {
        db.close()
    }

    @Test
    fun upsert_same_code_updates_instead_of_inserting_duplicate() = runTest {
        // given
        repo.upsert(Fiat(code = "USD", name = "Dólar", symbol = "$"))
        val before = repo.countAll()
        assertEquals(1, before)

        // when
        repo.upsert(Fiat(code = "USD", name = "US Dollar", symbol = "USD$"))

        // then
        val after = repo.countAll()
        assertEquals(1, after)

        val usd = repo.findByCode("USD")
        requireNotNull(usd)
        assertEquals("US Dollar", usd.name)
        assertEquals("USD$", usd.symbol)
    }

    @Test
    fun delete_non_existing_returns_false_or_noop() = runTest {
        // Arrange
        val before = repo.countAll()

        // Act
        val result = repo.delete("ZZZ") // código inexistente

        // Assert
        assertEquals(false, result) // o assertFalse(result)
        assertEquals(before, repo.countAll())
    }

    @Test
    fun getAll_returns_sorted_by_code() = runTest {
        repo.upsert(Fiat(code = "MXN", name = "Peso", symbol = "$"))
        repo.upsert(Fiat(code = "USD", name = "Dólar", symbol = "$"))
        repo.upsert(Fiat(code = "EUR", name = "Euro", symbol = "€"))

        val all = repo.getAll()

        val codes = all.map { it.code }
        assertEquals(listOf("EUR", "MXN", "USD"), codes)
    }
    @Test
    fun upsertAll_getAll_ordering_findByCode_replace() = runTest {
        repo.upsertAll(
            listOf(
                Fiat(code = "USD", name = "US Dollar", symbol = "$"),
                Fiat(code = "MXN", name = "Mexican Peso", symbol = "$")
            )
        )

        val all = repo.getAll()
        assertEquals(2, all.size)
        // ORDER BY code ASC (lo define el DAO)
        assertEquals("MXN", all[0].code)
        assertEquals("USD", all[1].code)

        val mxn = repo.findByCode("MXN")
        assertNotNull(mxn)
        assertEquals("Mexican Peso", mxn!!.name)

        // replace
        repo.upsertAll(listOf(Fiat(code = "MXN", name = "Peso Mexicano", symbol = "$")))
        val mxn2 = repo.findByCode("MXN")
        assertEquals("Peso Mexicano", mxn2!!.name)
    }

    @Test
    fun exists_returnsTrue_whenPresent() = runTest {
        assertFalse(repo.exists("EUR"))

        repo.upsertAll(listOf(Fiat(code = "EUR", name = "Euro", symbol = "€")))
        assertTrue(repo.exists("EUR"))
    }
}