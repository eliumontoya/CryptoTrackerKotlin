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

data class CatalogStatus(
    val portfolios: Int,
    val wallets: Int,
    val cryptos: Int,
    val fiat: Int
)



class CatalogSeeder(
    private val db: AppDatabase
) {
    suspend fun status(): CatalogStatus {
        return CatalogStatus(
            portfolios = db.portfolioDao().countAll(),
            wallets = db.walletDao().countAll(),
            cryptos = db.cryptoDao().countAll(),
            fiat = db.fiatDao().countAll()
        )
    }

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

        // WALLET seed solo si pidieron y NO hay wallets
       // if (req.wallets && walletDao.countAll() == 0) {
        if (req.wallets ) {
            val existingDefault = portfolioDao.getDefault()
            portfolioId = existingDefault?.portfolioId ?: portfolioDao.insert(InitialCatalogSeed.defaultPortfolio)

            walletDao.upsertAll(InitialCatalogSeed.walletsFor(portfolioId))
            walletsInserted = InitialCatalogSeed.walletsFor(portfolioId).size

        }

        // CRYPTO seed solo si pidieron y NO hay cryptos && cryptoDao.countAll() == 0
        if (req.cryptos ) {
            cryptoDao.upsertAll(InitialCatalogSeed.cryptos)
            cryptosUpserted = InitialCatalogSeed.cryptos.size
        }

        // FIAT seed solo si pidieron y NO hay fiat && fiatDao.countAll() == 0
        if (req.fiat ) {
            fiatDao.upsertAll(InitialCatalogSeed.fiat)
            fiatUpserted = InitialCatalogSeed.fiat.size
        }

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