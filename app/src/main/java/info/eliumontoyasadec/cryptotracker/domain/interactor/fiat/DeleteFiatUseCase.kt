package info.eliumontoyasadec.cryptotracker.domain.interactor.fiat

import info.eliumontoyasadec.cryptotracker.domain.repositories.FiatRepository

class DeleteFiatUseCase(
    private val repo: FiatRepository
) {
    suspend fun execute(cmd: DeleteFiatCommand): DeleteFiatResult {
        val code = cmd.codeRaw.trim().uppercase()
        if (code.isBlank()) return DeleteFiatResult.Failure("Código inválido.", safeGetAll())

        return try {
            val deleted = repo.delete(code)
            val items = repo.getAll()
            if (deleted) DeleteFiatResult.Deleted(items)
            else DeleteFiatResult.NotFound(items)
        } catch (t: Throwable) {
            DeleteFiatResult.Failure(t.message ?: "Fallo desconocido", safeGetAll())
        }
    }

    private suspend fun safeGetAll() = try {
        repo.getAll()
    } catch (_: Throwable) {
        emptyList()
    }
}