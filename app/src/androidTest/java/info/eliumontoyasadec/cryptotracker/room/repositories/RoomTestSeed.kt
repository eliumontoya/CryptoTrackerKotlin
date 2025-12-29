// File: src/androidTest/java/info/eliumontoyasadec/cryptotracker/room/testutil/RoomTestSeed.kt
package info.eliumontoyasadec.cryptotracker.room

import android.content.ContentValues
import android.database.sqlite.SQLiteDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import kotlin.io.use

object RoomTestSeed {

    fun enableForeignKeys(sqlDb: SupportSQLiteDatabase) {
        sqlDb.execSQL("PRAGMA foreign_keys=ON")
    }

    fun seedParents(
        sqlDb: SupportSQLiteDatabase,
        portfolioId: Long,
        walletId: Long,
        assetId: String
    ) {
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

        ensureRowExists(
            sqlDb = sqlDb,
            table = "cryptos",
            whereClause = "symbol = ?",
            whereArgs = arrayOf(assetId)
        ) { cv ->
            cv.put("symbol", assetId)
            cv.put("name", assetId.uppercase())
            cv.put("coingeckoId", assetId)
            cv.put("isActive", 1)
        }
    }

    inline fun ensureRowExists(
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

    fun fillMissingNotNulls(
        sqlDb: SupportSQLiteDatabase,
        table: String,
        cv: ContentValues
    ) {
        sqlDb.query("PRAGMA table_info($table)").use { cursor ->
            val nameIdx = cursor.getColumnIndex("name")
            val typeIdx = cursor.getColumnIndex("type")
            val notNullIdx = cursor.getColumnIndex("notnull")
            val dfltIdx = cursor.getColumnIndex("dflt_value")

            while (cursor.moveToNext()) {
                val colName = cursor.getString(nameIdx)
                val colType = (cursor.getString(typeIdx) ?: "TEXT")
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
}