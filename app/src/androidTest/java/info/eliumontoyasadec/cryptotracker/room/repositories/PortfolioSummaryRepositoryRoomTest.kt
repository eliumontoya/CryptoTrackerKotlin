package info.eliumontoyasadec.cryptotracker.room.repositories

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import info.eliumontoyasadec.cryptotracker.room.db.AppDatabase
import info.eliumontoyasadec.cryptotracker.room.entities.CryptoEntity
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
class PortfolioSummaryRepositoryRoomTest {

    private lateinit var db: AppDatabase

    private lateinit var holdingRepo: HoldingRepositoryRoom
    private lateinit var summaryRepo: PortfolioSummaryRepositoryRoom

    private var portfolioId: Long = 0L
    private var walletA: Long = 0L
    private var walletB: Long = 0L

    @Before
    fun setup() = runTest {
        db = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            AppDatabase::class.java
        ).allowMainThreadQueries().build()

        RoomTestSeed.enableForeignKeys(db.openHelper.writableDatabase)

        portfolioId = db.portfolioDao().insert(
            PortfolioEntity(name = "Main", description = null, isDefault = true)
        )

        walletA = db.walletDao().insert(
            WalletEntity(portfolioId = portfolioId, name = "Wallet A", description = null, isMain = true)
        )

        walletB = db.walletDao().insert(
            WalletEntity(portfolioId = portfolioId, name = "Wallet B", description = null, isMain = false)
        )

        db.cryptoDao().upsertAll(
            listOf(
                CryptoEntity(symbol = "btc", name = "Bitcoin", coingeckoId = "bitcoin", isActive = true),
                CryptoEntity(symbol = "eth", name = "Ethereum", coingeckoId = "ethereum", isActive = true)
            )
        )

        holdingRepo = HoldingRepositoryRoom(db.holdingDao())
        summaryRepo = PortfolioSummaryRepositoryRoom(db.portfolioSummaryDao())
    }

    @After
    fun tearDown() = db.close()

    @Test
    fun portfolio_summary_totals_and_wallet_breakdown_and_holdings_lists() = runTest {
        val t1 = 1700000000000L
        val t2 = 1700000001000
        val t3 = 1700000002000L

        // Holdings:
        holdingRepo.upsert(portfolioId, walletA, "btc", newQuantity = 1.0, updatedAt = t1)
        holdingRepo.upsert(portfolioId, walletA, "eth", newQuantity = 2.0, updatedAt = t2)
        holdingRepo.upsert(portfolioId, walletB, "btc", newQuantity = 3.0, updatedAt = t3)

        // 1) Portfolio total
        val total = summaryRepo.getPortfolioTotal(portfolioId)
        assertNotNull(total)
        assertEquals(portfolioId, total!!.portfolioId)
        assertEquals("Main", total.portfolioName)
        assertEquals(2, total.totalWallets)
        assertEquals(2, total.totalDistinctCryptos) // btc + eth
        assertEquals(t3, total.lastUpdatedAt) // max(updatedAt)

        // 2) Totales por wallet (orden: isMain DESC, name ASC)
        val walletTotals = summaryRepo.getWalletTotalsByPortfolio(portfolioId)
        assertEquals(2, walletTotals.size)
        assertEquals(walletA, walletTotals[0].walletId) // isMain=true primero
        assertEquals(2, walletTotals[0].totalDistinctCryptos)
        assertEquals(t2, walletTotals[0].lastUpdatedAt)

        assertEquals(walletB, walletTotals[1].walletId)
        assertEquals(1, walletTotals[1].totalDistinctCryptos)
        assertEquals(t3, walletTotals[1].lastUpdatedAt)

        // 3) Holdings por wallet
        val holdingsA = summaryRepo.getHoldingsByWallet(walletA)
        assertEquals(2, holdingsA.size)
        assertEquals(walletA, holdingsA[0].walletId)
        assertNotNull(holdingsA[0].cryptoName) // viene de cryptos (LEFT JOIN)

        val holdingsB = summaryRepo.getHoldingsByWallet(walletB)
        assertEquals(1, holdingsB.size)
        assertEquals("btc", holdingsB[0].assetId)

        // 4) Holdings por portfolio (todas las wallets)
        val holdingsAll = summaryRepo.getHoldingsByPortfolio(portfolioId)
        assertEquals(3, holdingsAll.size)
    }
}