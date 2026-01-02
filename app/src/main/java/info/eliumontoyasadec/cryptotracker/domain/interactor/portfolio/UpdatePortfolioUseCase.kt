package info.eliumontoyasadec.cryptotracker.domain.interactor.portfolio

import info.eliumontoyasadec.cryptotracker.domain.model.Portfolio
import info.eliumontoyasadec.cryptotracker.domain.repositories.PortfolioRepository

class UpdatePortfolioUseCase(
    private val repo: PortfolioRepository
) {
    suspend fun execute(cmd: UpdatePortfolioCommand): UpdatePortfolioResult {
        val name = cmd.nameRaw.trim()
        val description = cmd.descriptionRaw?.trim()?.takeIf { it.isNotBlank() }

        if (cmd.id <= 0) return UpdatePortfolioResult.ValidationError("Id inválido.")
        if (name.isBlank()) return UpdatePortfolioResult.ValidationError("El nombre es obligatorio.")

        return try {
            repo.update(
                Portfolio(
                    portfolioId = cmd.id,
                    name = name,
                    description = description,
                    isDefault = cmd.makeDefault
                )
            )
            UpdatePortfolioResult.Success(items = repo.getAll())
        } catch (t: Throwable) {
            UpdatePortfolioResult.Failure(t.message ?: "Fallo desconocido")
        }
    }
}