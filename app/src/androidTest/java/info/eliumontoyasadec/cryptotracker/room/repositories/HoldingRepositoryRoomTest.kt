package info.eliumontoyasadec.cryptotracker.room.repositories

import androidx.room.Room
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import info.eliumontoyasadec.cryptotracker.room.db.AppDatabase
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import info.eliumontoyasadec.cryptotracker.room.RoomTestSeed

@RunWith(AndroidJUnit4::class)
class HoldingRepositoryRoomTest {

    private lateinit var db: AppDatabase
    private lateinit var sqlDb: SupportSQLiteDatabase

    private lateinit var holdingRepo: HoldingRepositoryRoom
    private lateinit var movementRepo: MovementRepositoryRoom

    // IDs “padre” coherentes con tu esquema (Long para portfolio/wallet; assetId String)
    private val portfolioId = 1L
    private val walletId = 10L
    private val assetId = "btc"

    @Before
    fun setup() {
        db = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            AppDatabase::class.java
        )
            .allowMainThreadQueries()
            .build()

        sqlDb = db.openHelper.writableDatabase
        sqlDb.execSQL("PRAGMA foreign_keys=ON")

        // Seed de tablas padre para cumplir FK antes de holdings/movements
        RoomTestSeed.seedParents(
            sqlDb = sqlDb,
            portfolioId = portfolioId,
            walletId = walletId,
            assetId = assetId
        )

        holdingRepo = HoldingRepositoryRoom(db.holdingDao())
        movementRepo = MovementRepositoryRoom(db.movementDao())
    }

    @After
    fun tearDown() {
        db.close()
    }

    /* =========================
       HOLDING REPOSITORY TESTS
       ========================= */

    @Test
    fun holding_upsert_and_findByWalletAsset_works() = runTest {
        val t1 = 1700000000000L

        val saved = holdingRepo.upsert(
            portfolioId = portfolioId,
            walletId = walletId,
            assetId = assetId,
            newQuantity = 2.5,
            updatedAt = t1
        )

        assertEquals("$portfolioId|$walletId|$assetId", saved.id)
        assertEquals(portfolioId, saved.portfolioId)
        assertEquals(walletId, saved.walletId)
        assertEquals(assetId, saved.assetId)
        assertEquals(2.5, saved.quantity, 0.0000001)
        assertEquals(t1, saved.updatedAt)

        val found = holdingRepo.findByWalletAsset(walletId, assetId)
        assertNotNull(found)
        assertEquals(saved.id, found!!.id)
        assertEquals(2.5, found.quantity, 0.0000001)
        assertEquals(t1, found.updatedAt)
    }

    @Test
    fun holding_upsert_replaces_quantity_same_key() = runTest {
        val t1 = 1700000000000L
        val t2 = 1700000001000L

        holdingRepo.upsert(
            portfolioId = portfolioId,
            walletId = walletId,
            assetId = assetId,
            newQuantity = 1.0,
            updatedAt = t1
        )

        holdingRepo.upsert(
            portfolioId = portfolioId,
            walletId = walletId,
            assetId = assetId,
            newQuantity = 3.0,
            updatedAt = t2
        )

        val found = holdingRepo.findByWalletAsset(walletId, assetId)
        assertNotNull(found)
        assertEquals(3.0, found!!.quantity, 0.0000001)
        assertEquals(t2, found.updatedAt)
    }

}
