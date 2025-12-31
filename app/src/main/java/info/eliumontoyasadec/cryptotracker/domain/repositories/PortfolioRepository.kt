package info.eliumontoyasadec.cryptotracker.domain.repositories

import info.eliumontoyasadec.cryptotracker.domain.model.Portfolio

interface PortfolioRepository {

    suspend fun exists(portfolioId: Long): Boolean

    suspend fun insert(portfolio: Portfolio): Long
    suspend fun findById(portfolioId: Long): Portfolio?
    suspend fun getAll(): List<Portfolio>
    suspend fun getDefault(): Portfolio?
    suspend fun update(portfolio: Portfolio)
    suspend fun delete(portfolioId: Long)
    suspend fun delete(portfolio: Portfolio)
    suspend fun isDefault(portfolioId: Long): Boolean
    suspend fun setDefault(portfolioId: Long)
}