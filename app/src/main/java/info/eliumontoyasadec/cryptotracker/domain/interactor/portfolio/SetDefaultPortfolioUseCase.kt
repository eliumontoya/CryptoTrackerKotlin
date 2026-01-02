package info.eliumontoyasadec.cryptotracker.domain.interactor.portfolio

import info.eliumontoyasadec.cryptotracker.domain.repositories.PortfolioRepository

class SetDefaultPortfolioUseCase(
    private val repo: PortfolioRepository
) {
    suspend fun execute(cmd: SetDefaultPortfolioCommand): SetDefaultPortfolioResult {
        val id = cmd.id
        if (id <= 0) return SetDefaultPortfolioResult.Failure("Id inválido.")

        return try {
            repo.setDefault(id)
            SetDefaultPortfolioResult.Success(items = repo.getAll())
        } catch (t: Throwable) {
            SetDefaultPortfolioResult.Failure(t.message ?: "Fallo desconocido")
        }
    }
}