package info.eliumontoyasadec.cryptotracker.ui.admin.fiat

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import info.eliumontoyasadec.cryptotracker.domain.interactor.fiat.DeleteFiatUseCase
import info.eliumontoyasadec.cryptotracker.domain.interactor.fiat.GetAllFiatsUseCase
import info.eliumontoyasadec.cryptotracker.domain.interactor.fiat.UpsertFiatUseCase
import info.eliumontoyasadec.cryptotracker.domain.repositories.FiatRepository

class AdminFiatViewModelFactory(
    private val repo: FiatRepository
) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        require(modelClass == AdminFiatViewModel::class.java) {
            "Unknown ViewModel: ${modelClass.name}"
        }

        val getAll = GetAllFiatsUseCase(repo)
        val upsert = UpsertFiatUseCase(repo)
        val delete = DeleteFiatUseCase(repo)

        return AdminFiatViewModel(
            getAllFiats = getAll,
            upsertFiat = upsert,
            deleteFiat = delete
        ) as T
    }
}