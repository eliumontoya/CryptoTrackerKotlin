package info.eliumontoyasadec.cryptotracker.domain.interactor.crypto

import info.eliumontoyasadec.cryptotracker.domain.model.Crypto
import info.eliumontoyasadec.cryptotracker.domain.repositories.CryptoRepository

class GetAllCryptosUseCase(
    private val repo: CryptoRepository
) {
    suspend fun execute(): List<Crypto> = repo.getAll()
}