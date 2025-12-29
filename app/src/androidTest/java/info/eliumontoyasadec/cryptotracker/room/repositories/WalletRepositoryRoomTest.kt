package info.eliumontoyasadec.cryptotracker.room.repositories

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import info.eliumontoyasadec.cryptotracker.domain.model.Wallet
import info.eliumontoyasadec.cryptotracker.room.db.AppDatabase
import info.eliumontoyasadec.cryptotracker.room.entities.PortfolioEntity
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
        )
            .allowMainThreadQueries()
            .build()

        repo = WalletRepositoryRoom(db.walletDao())

        // Seed mínimo para FK: Portfolio
        portfolioId = db.portfolioDao().insert(
            PortfolioEntity(
                name = "P1",
                description = "Seed portfolio",
                isDefault = true
            )
        )
        assertTrue("Portfolio seed debe generar ID > 0", portfolioId > 0L)
    }

    @After
    fun teardown() {
        db.close()
    }

    @Test
    fun insert_findById_getByPortfolio_update_delete_happyPath() = runTest {
        // Insert 2 wallets
        val id1 = repo.insert(
            Wallet(
                walletId = 0L,
                portfolioId = portfolioId,
                name = "Zeta",
                description = "Z wallet",
                isMain = false
            )
        )
        val id2 = repo.insert(
            Wallet(
                walletId = 0L,
                portfolioId = portfolioId,
                name = "Alpha",
                description = null,
                isMain = true
            )
        )

        assertTrue(id1 > 0L)
        assertTrue(id2 > 0L)

        // getByPortfolio ordena isMain DESC, name ASC (según DAO)
        val list = repo.getByPortfolio(portfolioId)
        assertEquals(2, list.size)
        assertTrue(list[0].isMain)
        assertEquals("Alpha", list[0].name)
        assertEquals("Zeta", list[1].name)

        // findById
        val byId = repo.findById(id1)
        assertNotNull(byId)
        assertEquals("Zeta", byId!!.name)
        assertEquals(portfolioId, byId.portfolioId)

        // update (cambia nombre y isMain)
        repo.update(
            byId.copy(
                name = "Zeta Updated",
                isMain = true
            )
        )
        val updated = repo.findById(id1)
        assertNotNull(updated)
        assertEquals("Zeta Updated", updated!!.name)
        assertTrue(updated.isMain)

        // delete por id
        repo.delete(id1)
        assertNull(repo.findById(id1))

        // El otro sigue existiendo
        assertNotNull(repo.findById(id2))
    }

    @Test
    fun exists_and_belongsToPortfolio() = runTest {
        val walletId = repo.insert(
            Wallet(
                walletId = 0L,
                portfolioId = portfolioId,
                name = "Main",
                description = null,
                isMain = true
            )
        )

        assertTrue(repo.exists(walletId))
        assertTrue(repo.belongsToPortfolio(walletId, portfolioId))
        assertFalse(repo.belongsToPortfolio(walletId, portfolioId + 999))
    }

    @Test
    fun update_nonExisting_noCrash_and_delete_nonExisting_noCrash() = runTest {
        // update sobre wallet inexistente: tu repo actual hace "return" silencioso
        repo.update(
            Wallet(
                walletId = 999999L,
                portfolioId = portfolioId,
                name = "Ghost",
                description = null,
                isMain = false
            )
        )

        // delete inexistente: igual, no debe tronar
        repo.delete(999999L)

        // DB sigue sana
        assertTrue(repo.getByPortfolio(portfolioId).isEmpty())
    }
}