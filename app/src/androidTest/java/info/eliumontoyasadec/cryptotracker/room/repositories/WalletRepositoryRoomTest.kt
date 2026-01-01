package info.eliumontoyasadec.cryptotracker.room.repositories

import androidx.room.Room
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import info.eliumontoyasadec.cryptotracker.room.db.AppDatabase
import info.eliumontoyasadec.cryptotracker.domain.model.Wallet
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class WalletRepositoryRoomTest {

    private lateinit var db: AppDatabase
    private lateinit var repo: WalletRepositoryRoom

    @Before
    fun setUp() {
        db = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(), AppDatabase::class.java
        ).allowMainThreadQueries().build()

        repo = WalletRepositoryRoom(db.walletDao())
    }

    @After
    fun tearDown() {
        db.close()
    }

    @Test
    fun insert_findById_getByPortfolio_update_delete_happyPath() = runBlocking {
        val portfolioId = 1L
        ensurePortfolio(db.openHelper.writableDatabase, portfolioId, name = "P1", isDefault = true)

        val id1 = repo.insert(
            Wallet(
                walletId = 0L,
                portfolioId = portfolioId,
                name = "Binance",
                description = "description",

                isMain = true
            )
        )

        val id2 = repo.insert(
            Wallet(
                walletId = 0L,
                portfolioId = portfolioId,
                name = "ByBit",
                description = "description",

                isMain = false
            )
        )

        // findById
        val w1 = repo.findById(id1)
        assertNotNull(w1)
        assertEquals("Binance", w1!!.name)
        assertTrue(w1.isMain)

        val w2 = repo.findById(id2)
        assertNotNull(w2)
        assertEquals("ByBit", w2!!.name)
        assertFalse(w2.isMain)

        // getByPortfolio (main primero)
        val list = repo.getByPortfolio(portfolioId)
        assertEquals(2, list.size)
        assertTrue(list.any { it.walletId == id1 })
        assertTrue(list.any { it.walletId == id2 })

        // update
        repo.update(
            Wallet(
                walletId = id2,
                portfolioId = portfolioId,
                name = "ByBit Updated",
                description = "description",
                isMain = false
            )
        )
        val updated = repo.findById(id2)
        assertEquals("ByBit Updated", updated!!.name)

        // delete
        repo.delete(id2)
        assertNull(repo.findById(id2))
        assertEquals(1, repo.getByPortfolio(portfolioId).size)
    }

    @Test
    fun exists_and_belongsToPortfolio() = runBlocking {
        val p1 = 1L
        val p2 = 2L
        ensurePortfolio(db.openHelper.writableDatabase, p1, name = "P1", isDefault = true)
        ensurePortfolio(db.openHelper.writableDatabase, p2, name = "P2", isDefault = false)

        val id = repo.insert(
            Wallet(
                walletId = 0L, portfolioId = p1, name = "Main Wallet", description = "description",

                isMain = true
            )
        )

        assertTrue(repo.exists(id))
        assertTrue(repo.belongsToPortfolio(id, p1))
        assertFalse(repo.belongsToPortfolio(id, p2))
    }

    @Test
    fun setMain_makes_only_one_wallet_main_per_portfolio() = runBlocking {
        val p1 = 1L
        ensurePortfolio(db.openHelper.writableDatabase, p1, name = "P1", isDefault = true)

        val id1 = repo.insert(
            Wallet(
                walletId = 0L, portfolioId = p1, name = "Wallet A", description = "description",

                isMain = true
            )
        )
        val id2 = repo.insert(
            Wallet(
                walletId = 0L, portfolioId = p1, name = "Wallet B", description = "description",

                isMain = false
            )
        )

        // Cambia el main
        repo.setMain(id2)

        val list = repo.getByPortfolio(p1)
        val w1 = list.first { it.walletId == id1 }
        val w2 = list.first { it.walletId == id2 }

        assertFalse(w1.isMain)
        assertTrue(w2.isMain)
    }

    /**
     * Inserta un Portfolio válido para satisfacer la FK de Wallet -> Portfolio.
     * No depende de PortfolioDao/PortfolioEntity: detecta tabla y columnas en runtime.
     */
    private fun ensurePortfolio(
        sqliteDb: SupportSQLiteDatabase, portfolioId: Long, name: String, isDefault: Boolean
    ) {
        val portfolioTable = findPortfolioTable(sqliteDb) ?: run {
            throw IllegalStateException(
                "No pude encontrar la tabla de portfolios en sqlite_master. " + "Necesito el nombre real de la tabla o el PortfolioEntity/Dao."
            )
        }

        // Si ya existe, no reinserta
        val pkCol = findPrimaryKeyColumn(sqliteDb, portfolioTable)
        val exists = sqliteDb.query(
            "SELECT 1 FROM $portfolioTable WHERE $pkCol = ? LIMIT 1",
            arrayOf(portfolioId.toString())
        ).use { it.moveToFirst() }
        if (exists) return

        val cols = tableColumns(sqliteDb, portfolioTable)

        // Armamos INSERT con lo que exista. PK siempre.
        val insertCols = mutableListOf<String>()
        val insertVals = mutableListOf<Any?>()

        insertCols += pkCol
        insertVals += portfolioId

        // Nombre (si existe)
        val nameCol = cols.firstOrNull { it.equals("name", true) } ?: cols.firstOrNull {
            it.contains(
                "name", true
            )
        }
        if (nameCol != null) {
            insertCols += nameCol
            insertVals += name
        }

        // Default (si existe)
        val defaultCol = cols.firstOrNull { it.equals("isDefault", true) }
            ?: cols.firstOrNull { it.contains("default", true) } ?: cols.firstOrNull {
                it.equals(
                    "isMain", true
                )
            } // por si alguien lo nombró mal en entidad
        if (defaultCol != null) {
            insertCols += defaultCol
            insertVals += if (isDefault) 1 else 0
        }

        // Para cualquier NOT NULL sin default (si existiera), intenta rellenar con 0/"".
        // (No lo hacemos agresivo: solo si detectamos que hay más columnas NOT NULL sin default)
        val requiredExtra =
            requiredNoDefaultColumns(sqliteDb, portfolioTable).filterNot { it.equals(pkCol, true) }
                .filterNot { insertCols.any { c -> c.equals(it, true) } }

        requiredExtra.forEach { col ->
            insertCols += col
            insertVals += 0
        }

        val placeholders = insertCols.joinToString(",") { "?" }
        val sql =
            "INSERT INTO $portfolioTable (${insertCols.joinToString(",")}) VALUES ($placeholders)"
        sqliteDb.execSQL(sql, insertVals.toTypedArray())
    }

    private fun findPortfolioTable(db: SupportSQLiteDatabase): String? {
        val candidates = mutableListOf<String>()
        db.query("SELECT name FROM sqlite_master WHERE type='table'").use { c ->
            while (c.moveToNext()) {
                val name = c.getString(0)
                if (name == "android_metadata" || name == "room_master_table") continue
                // heuristic: contiene "portfolio"
                if (name.contains("portfolio", ignoreCase = true)) {
                    candidates += name
                }
            }
        }
        // si hay varias, toma la más “simple”
        return candidates.minByOrNull { it.length }
    }

    private fun tableColumns(db: SupportSQLiteDatabase, table: String): List<String> {
        val cols = mutableListOf<String>()
        db.query("PRAGMA table_info($table)").use { c ->
            while (c.moveToNext()) {
                cols += c.getString(c.getColumnIndexOrThrow("name"))
            }
        }
        return cols
    }

    private fun findPrimaryKeyColumn(db: SupportSQLiteDatabase, table: String): String {
        db.query("PRAGMA table_info($table)").use { c ->
            while (c.moveToNext()) {
                val pk = c.getInt(c.getColumnIndexOrThrow("pk"))
                if (pk == 1) return c.getString(c.getColumnIndexOrThrow("name"))
            }
        }
        // fallback típico
        return "portfolioId"
    }

    private fun requiredNoDefaultColumns(db: SupportSQLiteDatabase, table: String): List<String> {
        val cols = mutableListOf<String>()
        db.query("PRAGMA table_info($table)").use { c ->
            while (c.moveToNext()) {
                val name = c.getString(c.getColumnIndexOrThrow("name"))
                val notNull = c.getInt(c.getColumnIndexOrThrow("notnull")) == 1
                val dflt = c.getString(c.getColumnIndexOrThrow("dflt_value"))
                val pk = c.getInt(c.getColumnIndexOrThrow("pk")) == 1
                if (!pk && notNull && dflt == null) cols += name
            }
        }
        return cols
    }
}