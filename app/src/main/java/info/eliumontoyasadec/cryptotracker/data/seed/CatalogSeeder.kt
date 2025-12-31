package info.eliumontoyasadec.cryptotracker.data.seed

import info.eliumontoyasadec.cryptotracker.room.db.AppDatabase

data class SeedRequest(
    val wallets: Boolean,
    val cryptos: Boolean,
    val fiat: Boolean,
    val syncManual: Boolean
)

data class SeedResult(
    val createdPortfolioId: Long?,
    val walletsInserted: Int,
    val cryptosUpserted: Int,
    val fiatUpserted: Int,
    val syncApplied: Boolean
)

class CatalogSeeder(
    private val db: AppDatabase
) {
    suspend fun seed(req: SeedRequest): SeedResult {
        val portfolioDao = db.portfolioDao()
        val walletDao = db.walletDao()
        val cryptoDao = db.cryptoDao()
        val fiatDao = db.fiatDao()

        var portfolioId: Long? = null
        var walletsInserted = 0
        var cryptosUpserted = 0
        var fiatUpserted = 0
        var syncApplied = false

        // Carteras implica: crear portafolio default (si no existe) y wallets
        if (req.wallets) {
            val existingDefault = portfolioDao.getDefault()
            portfolioId = existingDefault?.portfolioId ?: portfolioDao.insert(InitialCatalogSeed.defaultPortfolio)

            InitialCatalogSeed.walletsFor(portfolioId).forEach {
                walletDao.insert(it)
                walletsInserted++
            }
        }

        if (req.cryptos) {
            cryptoDao.upsertAll(InitialCatalogSeed.cryptos)
            cryptosUpserted = InitialCatalogSeed.cryptos.size
        }

        if (req.fiat) {
            fiatDao.upsertAll(InitialCatalogSeed.fiat)
            fiatUpserted = InitialCatalogSeed.fiat.size
        }

        // SyncManual por ahora es un “flag” listo para crecer (tabla/config futura)
        if (req.syncManual) {
            syncApplied = true
        }

        return SeedResult(
            createdPortfolioId = portfolioId,
            walletsInserted = walletsInserted,
            cryptosUpserted = cryptosUpserted,
            fiatUpserted = fiatUpserted,
            syncApplied = syncApplied
        )
    }
}