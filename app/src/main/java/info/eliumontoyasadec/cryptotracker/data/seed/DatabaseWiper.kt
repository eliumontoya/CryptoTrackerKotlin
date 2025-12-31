package info.eliumontoyasadec.cryptotracker.data.seed

import android.util.Log
import info.eliumontoyasadec.cryptotracker.room.db.AppDatabase

data class DeleteRequest(
    val all: Boolean,
    val wallets: Boolean,
    val cryptos: Boolean,
    val fiat: Boolean,
    val movements: Boolean,
    val holdings: Boolean,
    val portfolio: Boolean
)

data class DeleteResult(
    val deletedWallets: Boolean,
    val deletedCryptos: Boolean,
    val deletedFiat: Boolean,
    val deletedMovements: Boolean,
    val deletedHoldings: Boolean,
    val deletedPortfolio: Boolean,
    val deletedAllTables: Boolean,
    // conteos:
    val walletsDeletedCount: Int = 0,
    val cryptosDeletedCount: Int = 0,
    val fiatDeletedCount: Int = 0,
    val movementsDeletedCount: Int = 0,
    val holdingsDeletedCount: Int = 0,
    val portfoliosDeletedCount: Int = 0
)

class DatabaseWiper(
    private val db: AppDatabase
) {
    suspend fun wipe(req: DeleteRequest): DeleteResult {

            // 1) Calcula los "a borrar" (si all, todo)
            val willDeleteMovements = req.all || req.movements
            val willDeleteHoldings = req.all || req.holdings
            val willDeleteWallets = req.all || req.wallets
            val willDeletePortfolio = req.all || req.portfolio
            val willDeleteCryptos = req.all || req.cryptos
            val willDeleteFiat = req.all || req.fiat

            // 2) Conteos ANTES (esto es lo que reportaremos como "borrado")
            //    Requiere que tus DAOs tengan countAll(): Int (suspend)
            val beforeMovements = if (willDeleteMovements) db.movementDao().countAll() else 0
            val beforeHoldings  = if (willDeleteHoldings)  db.holdingDao().countAll() else 0
            val beforeWallets   = if (willDeleteWallets)   db.walletDao().countAll() else 0
            val beforePortfolio = if (willDeletePortfolio) db.portfolioDao().countAll() else 0
            val beforeCryptos   = if (willDeleteCryptos)   db.cryptoDao().countAll() else 0
            val beforeFiat      = if (willDeleteFiat)      db.fiatDao().countAll() else 0

            // 3) Borrado
            try {
                if (req.all) {
                    // rápido y seguro
                    db.clearAllTables()
                } else {
                    // orden: hijos -> padres, catálogos al final
                    if (req.movements) db.movementDao().deleteAll()
                    if (req.holdings) db.holdingDao().deleteAll()

                    if (req.wallets) db.walletDao().deleteAll()
                    if (req.portfolio) db.portfolioDao().deleteAll()

                    if (req.cryptos) db.cryptoDao().deleteAll()
                    if (req.fiat) db.fiatDao().deleteAll()
                }
            } catch (t: Throwable) {
                Log.e("DatabaseWiper", "wipe failed req=$req", t)
                throw t
            }

            // 4) Resultado (bool + conteos)
            return DeleteResult(
                deletedWallets = willDeleteWallets,
                deletedCryptos = willDeleteCryptos,
                deletedFiat = willDeleteFiat,
                deletedMovements = willDeleteMovements,
                deletedHoldings = willDeleteHoldings,
                deletedPortfolio = willDeletePortfolio,
                deletedAllTables = req.all,

                walletsDeletedCount = beforeWallets,
                cryptosDeletedCount = beforeCryptos,
                fiatDeletedCount = beforeFiat,
                movementsDeletedCount = beforeMovements,
                holdingsDeletedCount = beforeHoldings,
                portfoliosDeletedCount = beforePortfolio
            )
        }
    }