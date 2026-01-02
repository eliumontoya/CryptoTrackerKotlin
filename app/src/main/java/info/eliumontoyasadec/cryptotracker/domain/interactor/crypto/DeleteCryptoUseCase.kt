package info.eliumontoyasadec.cryptotracker.domain.interactor.crypto

import info.eliumontoyasadec.cryptotracker.domain.repositories.CryptoRepository

class DeleteCryptoUseCase(
    private val repo: CryptoRepository
) {
    //    suspend fun execute(cmd: UpsertCryptoCommand): UpsertCryptoResult {
    suspend fun execute(cmd: DeleteCryptoCommand): DeleteCryptoResult {
        val symbol = cmd.symbolRaw.trim().uppercase()
        if (symbol.isBlank()) {
            val items = safeGetAll()
            return DeleteCryptoResult.Failure("Símbolo inválido.", items)
        }

        return try {
            val deleted = repo.deleteBySymbol(symbol)
            val items = repo.getAll()
            if (deleted > 0) DeleteCryptoResult.Deleted(items)
            else DeleteCryptoResult.NotFound(items)
        } catch (t: Throwable) {
            // Si hay FK constraint (holdings/movements), lo elevamos a resultado de negocio
            val items = safeGetAll()
            DeleteCryptoResult.InUse(
                message = t.message ?: "No se pudo eliminar (posible relación con otros datos).",
                items = items
            )
        }
    }

    private suspend fun safeGetAll() = try {
        repo.getAll()
    } catch (_: Throwable) {
        emptyList()
    }
}