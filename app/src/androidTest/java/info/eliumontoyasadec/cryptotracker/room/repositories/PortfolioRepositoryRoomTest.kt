package info.eliumontoyasadec.cryptotracker.room.repositories

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import info.eliumontoyasadec.cryptotracker.room.db.AppDatabase
import info.eliumontoyasadec.cryptotracker.room.entities.PortfolioEntity
import info.eliumontoyasadec.cryptotracker.room.RoomTestSeed
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class PortfolioRepositoryRoomTest {

    private lateinit var db: AppDatabase
    private lateinit var repo: PortfolioRepositoryRoom

    @Before
    fun setup() {
        db = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            AppDatabase::class.java
        ).allowMainThreadQueries().build()

        RoomTestSeed.enableForeignKeys(db.openHelper.writableDatabase)

        repo = PortfolioRepositoryRoom(db.portfolioDao())
    }

    @After
    fun tearDown() = db.close()

    @Test
    fun insert_getById_getAll_ordering_getDefault_update_delete() = runTest {
        val idA = repo.insert(
            PortfolioEntity(
                name = "Alpha",
                description = null,
                isDefault = false
            )
        )

        val idB = repo.insert(
            PortfolioEntity(
                name = "Beta",
                description = "desc",
                isDefault = true
            )
        )

        val all = repo.getAll()
        assertEquals(2, all.size)
        // ORDER BY isDefault DESC, name ASC
        assertEquals(idB, all[0].portfolioId)
        assertEquals("Beta", all[0].name)

        val def = repo.getDefault()
        assertNotNull(def)
        assertEquals(idB, def!!.portfolioId)

        val byId = repo.getById(idA)
        assertNotNull(byId)
        assertEquals("Alpha", byId!!.name)

        repo.update(byId.copy(name = "Alpha Updated"))
        val updated = repo.getById(idA)
        assertEquals("Alpha Updated", updated!!.name)

        repo.delete(updated)
        assertNull(repo.getById(idA))
    }
}