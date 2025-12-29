package info.eliumontoyasadec.cryptotracker.room.repositories

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import info.eliumontoyasadec.cryptotracker.room.db.AppDatabase
import info.eliumontoyasadec.cryptotracker.room.entities.FiatEntity
import info.eliumontoyasadec.cryptotracker.room.RoomTestSeed
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
        ).allowMainThreadQueries().build()

        RoomTestSeed.enableForeignKeys(db.openHelper.writableDatabase)

        repo = FiatRepositoryRoom(db.fiatDao())
    }

    @After
    fun tearDown() = db.close()

    @Test
    fun upsertAll_getAll_ordering_getByCode_replace() = runTest {
        repo.upsertAll(
            listOf(
                FiatEntity(code = "USD", name = "US Dollar", symbol = "$"),
                FiatEntity(code = "MXN", name = "Mexican Peso", symbol = "$")
            )
        )

        val all = repo.getAll()
        assertEquals(2, all.size)
        // ORDER BY code ASC
        assertEquals("MXN", all[0].code)
        assertEquals("USD", all[1].code)

        val mxn = repo.getByCode("MXN")
        assertNotNull(mxn)
        assertEquals("Mexican Peso", mxn!!.name)

        repo.upsertAll(listOf(FiatEntity(code = "MXN", name = "Peso Mexicano", symbol = "$")))
        val mxn2 = repo.getByCode("MXN")
        assertEquals("Peso Mexicano", mxn2!!.name)
    }
}