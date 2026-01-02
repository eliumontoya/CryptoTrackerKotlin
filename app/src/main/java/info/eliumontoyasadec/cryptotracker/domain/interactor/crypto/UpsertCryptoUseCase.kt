package info.eliumontoyasadec.cryptotracker.domain.interactor.crypto

import info.eliumontoyasadec.cryptotracker.domain.model.Crypto
import info.eliumontoyasadec.cryptotracker.domain.repositories.CryptoRepository

class UpsertCryptoUseCase(
    private val repo: CryptoRepository
) {
    suspend fun execute(cmd: UpsertCryptoCommand): UpsertCryptoResult {
        val symbol = cmd.symbolRaw.trim().uppercase()
        val name = cmd.nameRaw.trim()

        if (symbol.isBlank()) return UpsertCryptoResult.ValidationError("El símbolo es obligatorio.")
        if (name.isBlank()) return UpsertCryptoResult.ValidationError("El nombre es obligatorio.")

        return try {
            repo.upsertOne(
                Crypto(
                    symbol = symbol,
                    name = name,
                    coingeckoId = null,
                    isActive = cmd.isActive
                )
            )
            val items = repo.getAll()
            UpsertCryptoResult.Success(items = items, wasUpdate = cmd.isEditing)
        } catch (t: Throwable) {
            UpsertCryptoResult.Failure(t.message ?: "Fallo desconocido")
        }
    }
}