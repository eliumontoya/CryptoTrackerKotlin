// File: room/repositories/PortfolioSummaryRoomRepository.kt
package info.eliumontoyasadec.cryptotracker.room.repositories

import info.eliumontoyasadec.cryptotracker.room.queries.PortfolioSummaryDao
import info.eliumontoyasadec.cryptotracker.room.queries.rows.PortfolioTotalRow
import info.eliumontoyasadec.cryptotracker.room.queries.rows.PortfolioWalletTotalRow
import info.eliumontoyasadec.cryptotracker.room.queries.rows.WalletHoldingRow

class PortfolioSummaryRepositoryRoom(
    private val dao: PortfolioSummaryDao
) {
    suspend fun getPortfolioTotal(portfolioId: Long): PortfolioTotalRow? =
        dao.getPortfolioTotal(portfolioId)

    suspend fun getWalletTotalsByPortfolio(portfolioId: Long): List<PortfolioWalletTotalRow> =
        dao.getWalletTotalsByPortfolio(portfolioId)

    suspend fun getHoldingsByWallet(walletId: Long): List<WalletHoldingRow> =
        dao.getHoldingsByWallet(walletId)

    suspend fun getHoldingsByPortfolio(portfolioId: Long): List<WalletHoldingRow> =
        dao.getHoldingsByPortfolio(portfolioId)
}