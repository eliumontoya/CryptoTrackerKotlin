package info.eliumontoyasadec.cryptotracker.room.repositories

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import info.eliumontoyasadec.cryptotracker.domain.model.Portfolio
import info.eliumontoyasadec.cryptotracker.room.db.AppDatabase
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
        )
            .allowMainThreadQueries()
            .build()

        repo = PortfolioRepositoryRoom(db.portfolioDao())
    }

    @After
    fun teardown() {
        db.close()
    }

    @Test
    fun insert_find_getAll_getDefault_update_delete_happyPath() = runTest {
        val id1 = repo.insert(
            Portfolio(
                portfolioId = 0L,
                name = "Main",
                description = "Main portfolio",
                isDefault = true
            )
        )
        val id2 = repo.insert(
            Portfolio(
                portfolioId = 0L,
                name = "Secondary",
                description = null,
                isDefault = false
            )
        )

        assertTrue(id1 > 0)
        assertTrue(id2 > 0)

        // getAll
        val all = repo.getAll()
        assertEquals(2, all.size)

        // getDefault
        val default = repo.getDefault()
        assertNotNull(default)
        assertTrue(default!!.isDefault)
        assertEquals("Main", default.name)

        // findById
        val byId = repo.findById(id2)
        assertNotNull(byId)
        assertEquals("Secondary", byId!!.name)

        // update
        repo.update(
            byId.copy(
                name = "Secondary Updated",
                isDefault = true
            )
        )

        val updated = repo.findById(id2)
        assertEquals("Secondary Updated", updated!!.name)
        assertTrue(updated.isDefault)

        // delete
        repo.delete(id1)
        assertNull(repo.findById(id1))
        assertNotNull(repo.findById(id2))
    }

    @Test
    fun exists_nonExisting_returnsFalse() = runTest {
        assertFalse(repo.exists(9999L))
    }
}