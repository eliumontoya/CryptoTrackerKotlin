package info.eliumontoyasadec.cryptotracker.domain.interactor.fiat

import info.eliumontoyasadec.cryptotracker.domain.model.Fiat
import info.eliumontoyasadec.cryptotracker.domain.repositories.FiatRepository

class GetAllFiatsUseCase(
    private val repo: FiatRepository
) {
    suspend fun execute(): List<Fiat> = repo.getAll()
}