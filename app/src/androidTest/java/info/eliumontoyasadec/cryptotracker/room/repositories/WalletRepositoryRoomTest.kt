package info.eliumontoyasadec.cryptotracker.room.repositories

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import info.eliumontoyasadec.cryptotracker.room.db.AppDatabase
import info.eliumontoyasadec.cryptotracker.room.entities.PortfolioEntity
import info.eliumontoyasadec.cryptotracker.room.entities.WalletEntity
import info.eliumontoyasadec.cryptotracker.room.RoomTestSeed
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class WalletRepositoryRoomTest {

    private lateinit var db: AppDatabase
    private lateinit var repo: WalletRepositoryRoom

    private var portfolioId: Long = 0L

    @Before
    fun setup() = runTest {
        db = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            AppDatabase::class.java
        ).allowMainThreadQueries().build()

        RoomTestSeed.enableForeignKeys(db.openHelper.writableDatabase)

        // parent: portfolio
        portfolioId = db.portfolioDao().insert(
            PortfolioEntity(name = "Main", description = null, isDefault = true)
        )

        repo = WalletRepositoryRoom(db.walletDao())
    }

    @After
    fun tearDown() = db.close()

    @Test
    fun insert_getById_getByPortfolio_ordering_update_delete() = runTest {
        val id1 = repo.insert(
            WalletEntity(
                portfolioId = portfolioId,
                name = "Zeta",
                description = null,
                isMain = false
            )
        )

        val id2 = repo.insert(
            WalletEntity(
                portfolioId = portfolioId,
                name = "Alpha",
                description = "desc",
                isMain = true
            )
        )

        val list = repo.getByPortfolio(portfolioId)
        assertEquals(2, list.size)
        // ORDER BY isMain DESC, name ASC
        assertEquals(id2, list[0].walletId)
        assertEquals("Alpha", list[0].name)

        val byId = repo.getById(id1)
        assertNotNull(byId)
        assertEquals("Zeta", byId!!.name)

        repo.update(byId.copy(name = "Zeta Updated", isMain = true))
        val updated = repo.getById(id1)
        assertEquals("Zeta Updated", updated!!.name)
        assertTrue(updated.isMain)

        repo.delete(updated)
        assertNull(repo.getById(id1))
    }
}