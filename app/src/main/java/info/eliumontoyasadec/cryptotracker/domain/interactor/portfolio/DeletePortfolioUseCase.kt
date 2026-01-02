package info.eliumontoyasadec.cryptotracker.domain.interactor.portfolio

import info.eliumontoyasadec.cryptotracker.domain.repositories.PortfolioRepository

class DeletePortfolioUseCase(
    private val repo: PortfolioRepository
) {
    suspend fun execute(cmd: DeletePortfolioCommand): DeletePortfolioResult {
        val id = cmd.id
        if (id <= 0) return DeletePortfolioResult.Failure("Id inválido.", safeGetAll())

        return try {
            repo.delete(id)
            DeletePortfolioResult.Success(items = repo.getAll())
        } catch (t: Throwable) {
            DeletePortfolioResult.Failure(t.message ?: "Fallo desconocido", safeGetAll())
        }
    }

    private suspend fun safeGetAll() = try { repo.getAll() } catch (_: Throwable) { emptyList() }
}