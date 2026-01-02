package info.eliumontoyasadec.cryptotracker.domain.interactor.fiat

import info.eliumontoyasadec.cryptotracker.domain.model.Fiat
import info.eliumontoyasadec.cryptotracker.domain.repositories.FiatRepository

class UpsertFiatUseCase(
    private val repo: FiatRepository
) {
    suspend fun execute(cmd: UpsertFiatCommand): UpsertFiatResult {
        val code = cmd.codeRaw.trim().uppercase()
        val name = cmd.nameRaw.trim()
        val symbol = (cmd.symbolRaw ?: "").trim()

        if (code.isBlank()) return UpsertFiatResult.ValidationError("El código no puede estar vacío")
        if (name.isBlank()) return UpsertFiatResult.ValidationError("El nombre no puede estar vacío")

        return try {
            repo.upsert(
                Fiat(
                    code = code,
                    name = name,
                    symbol = symbol
                )
            )
            val items = repo.getAll()
            UpsertFiatResult.Success(items = items, wasUpdate = cmd.isEditing)
        } catch (t: Throwable) {
            UpsertFiatResult.Failure(t.message ?: "Fallo desconocido")
        }
    }
}