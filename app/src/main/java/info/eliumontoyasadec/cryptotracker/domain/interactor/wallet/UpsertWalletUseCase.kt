package info.eliumontoyasadec.cryptotracker.domain.interactor.wallet

import info.eliumontoyasadec.cryptotracker.domain.model.Wallet
import info.eliumontoyasadec.cryptotracker.domain.repositories.WalletRepository

class UpsertWalletUseCase(
    private val walletRepo: WalletRepository
) {
    suspend fun execute(cmd: UpsertWalletCommand): UpsertWalletResult {
        val name = cmd.nameRaw.trim()
        if (cmd.portfolioId <= 0) return UpsertWalletResult.ValidationError("Portfolio inválido.")
        if (name.isBlank()) return UpsertWalletResult.ValidationError("El nombre no puede estar vacío.")

        return try {
            val editingId = cmd.walletId
            if (editingId == null) {
                val newId = walletRepo.insert(
                    Wallet(
                        walletId = 0L,
                        portfolioId = cmd.portfolioId,
                        name = name,
                        description = "",
                        isMain = cmd.makeMain
                    )
                )
                if (cmd.makeMain) walletRepo.setMain(newId)
            } else {
                walletRepo.update(editingId, name)
                if (cmd.makeMain) walletRepo.setMain(editingId)
            }

            val items = walletRepo.getByPortfolio(cmd.portfolioId)
            UpsertWalletResult.Success(items)
        } catch (t: Throwable) {
            UpsertWalletResult.Failure(t.message ?: "Fallo desconocido")
        }
    }
}