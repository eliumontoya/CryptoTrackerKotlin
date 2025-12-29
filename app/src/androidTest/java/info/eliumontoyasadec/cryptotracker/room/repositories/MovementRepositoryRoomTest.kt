package info.eliumontoyasadec.cryptotracker.room.repositories

import androidx.room.Room
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import info.eliumontoyasadec.cryptotracker.domain.model.Movement
import info.eliumontoyasadec.cryptotracker.domain.model.MovementType
import info.eliumontoyasadec.cryptotracker.room.RoomTestSeed
import info.eliumontoyasadec.cryptotracker.room.db.AppDatabase
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class MovementRepositoryRoomTest {


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



    /* ==========================
       MOVEMENT REPOSITORY TESTS
       ========================== */

    @Test
    fun movement_insert_find_update_delete_works_with_explicit_id() = runTest {
        val now = 1700000000000L

        // IMPORTANTÍSIMO:
        // Con tu implementación actual, si id=0 Room puede generar uno,
        // pero tu repo NO lo recupera. Así que aquí usamos un id explícito.
        val fixedId = 1001L

        val insertedId = movementRepo.insert(
            Movement(
                id = fixedId,
                portfolioId = portfolioId,
                walletId = walletId,
                assetId = assetId,
                type = MovementType.BUY,
                quantity = 1.0,
                price = 40000.0,
                feeQuantity = 0.0,
                timestamp = now,
                notes = "test",
                groupId = 1L
            )
        )
        assertEquals(fixedId, insertedId)

        val found = movementRepo.findById(fixedId)
        assertNotNull(found)
        assertEquals(fixedId, found!!.id)
        assertEquals(portfolioId, found.portfolioId)
        assertEquals(walletId, found.walletId)
        assertEquals(assetId, found.assetId)
        assertEquals(MovementType.BUY, found.type)
        assertEquals(1.0, found.quantity, 0.0000001)
        assertEquals(40000.0, found.price!!, 0.0000001)
        assertEquals(0.0, found.feeQuantity, 0.0000001)
        assertEquals(now, found.timestamp)
        assertEquals("test", found.notes)
        assertEquals(1L, found.groupId)

        // Update: cambia valores y valida tu merge de groupId (null => conserva old.groupId)
        movementRepo.update(
            movementId = fixedId,
            update = found.copy(
                type = MovementType.SELL,
                quantity = 2.0,
                price = 41000.0,
                feeQuantity = 0.01,
                timestamp = now + 1,
                notes = "updated",
                groupId = null // debe conservar 1L
            )
        )

        val afterUpdate = movementRepo.findById(fixedId)
        assertNotNull(afterUpdate)
        assertEquals(MovementType.SELL, afterUpdate!!.type)
        assertEquals(2.0, afterUpdate.quantity, 0.0000001)
        assertEquals(41000.0, afterUpdate.price!!, 0.0000001)
        assertEquals(0.01, afterUpdate.feeQuantity, 0.0000001)
        assertEquals(now + 1, afterUpdate.timestamp)
        assertEquals("updated", afterUpdate.notes)
        assertEquals(1L, afterUpdate.groupId) // ✅ se conserva

        movementRepo.delete(fixedId)
        val afterDelete = movementRepo.findById(fixedId)
        assertNull(afterDelete)
    }
}

/*

/* =========================================================
   SEEDING: respeta FK + NOT NULL + tipos (Long vs String)
   ========================================================= */

fun seedParents(
    sqlDb: SupportSQLiteDatabase,
    portfolioId: Long,
    walletId: Long,
    assetId: String
) {
    // 1) portfolios
    ensureRowExists(
        sqlDb = sqlDb,
        table = "portfolios",
        whereClause = "portfolioId = ?",
        whereArgs = arrayOf(portfolioId.toString())
    ) { cv ->
        cv.put("portfolioId", portfolioId)
        cv.put("name", "Main")
        cv.put("isDefault", 1)
    }

    // 2) wallets
    ensureRowExists(
        sqlDb = sqlDb,
        table = "wallets",
        whereClause = "walletId = ?",
        whereArgs = arrayOf(walletId.toString())
    ) { cv ->
        cv.put("walletId", walletId)
        cv.put("portfolioId", portfolioId)
        cv.put("name", "Wallet 1")
        cv.put("isMain", 1)
    }

    // 3) cryptos
    ensureRowExists(
        sqlDb = sqlDb,
        table = "cryptos",
        whereClause = "symbol = ?",
        whereArgs = arrayOf(assetId)
    ) { cv ->
        cv.put("symbol", assetId)
        cv.put("name", assetId.uppercase())
        // NOT NULL típicos en tu entidad: coingeckoId/isActive
        cv.put("coingeckoId", assetId)
        cv.put("isActive", 1)
    }
}

private inline fun ensureRowExists(
    sqlDb: SupportSQLiteDatabase,
    table: String,
    whereClause: String,
    whereArgs: Array<String>,
    fill: (ContentValues) -> Unit
) {
    val exists = sqlDb.query(
        "SELECT 1 FROM $table WHERE $whereClause LIMIT 1",
        whereArgs
    ).use { c -> c.moveToFirst() }

    if (exists) return

    val cv = ContentValues().apply(fill)

    fillMissingNotNulls(sqlDb, table, cv)

    sqlDb.insert(table, SQLiteDatabase.CONFLICT_IGNORE, cv)
}

private fun fillMissingNotNulls(
    sqlDb: SupportSQLiteDatabase,
    table: String,
    cv: ContentValues
) {
    val pragma = "PRAGMA table_info($table)"
    sqlDb.query(pragma).use { cursor ->
        val nameIdx = cursor.getColumnIndex("name")
        val typeIdx = cursor.getColumnIndex("type")
        val notNullIdx = cursor.getColumnIndex("notnull")
        val dfltIdx = cursor.getColumnIndex("dflt_value")

        while (cursor.moveToNext()) {
            val colName = cursor.getString(nameIdx)
            val colType = cursor.getString(typeIdx) ?: "TEXT"
            val notNull = cursor.getInt(notNullIdx) == 1
            val hasDefault = !cursor.isNull(dfltIdx)

            if (!notNull) continue
            if (hasDefault) continue
            if (cv.containsKey(colName)) continue

            when {
                colType.contains("INT", ignoreCase = true) -> cv.put(colName, 0L)
                colType.contains("REAL", ignoreCase = true) ||
                        colType.contains("FLOA", ignoreCase = true) ||
                        colType.contains("DOUB", ignoreCase = true) -> cv.put(colName, 0.0)
                else -> cv.put(colName, "")
            }
        }
    }
}

private inline fun <T> Cursor.use(block: (Cursor) -> T): T =
    try { block(this) } finally { close() }

 */