package info.eliumontoyasadec.cryptotracker.domain.interactor.portfolio

import info.eliumontoyasadec.cryptotracker.domain.model.Portfolio
import info.eliumontoyasadec.cryptotracker.domain.repositories.PortfolioRepository

class CreatePortfolioUseCase(
    private val repo: PortfolioRepository
) {
    suspend fun execute(cmd: CreatePortfolioCommand): CreatePortfolioResult {
        val name = cmd.nameRaw.trim()
        val description = cmd.descriptionRaw?.trim()?.takeIf { it.isNotBlank() }

        if (name.isBlank()) return CreatePortfolioResult.ValidationError("El nombre es obligatorio.")

        return try {
            repo.insert(
                Portfolio(
                    name = name,
                    description = description,
                    isDefault = cmd.makeDefault
                )
            )
            CreatePortfolioResult.Success(items = repo.getAll())
        } catch (t: Throwable) {
            CreatePortfolioResult.Failure(t.message ?: "Fallo desconocido")
        }
    }
}