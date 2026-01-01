package info.eliumontoyasadec.cryptotracker.ui.admin.fiat

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import info.eliumontoyasadec.cryptotracker.domain.repositories.FiatRepository

class AdminFiatViewModelFactory(
    private val repo: FiatRepository
) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        require(modelClass == AdminFiatViewModel::class.java) {
            "Unknown ViewModel: ${modelClass.name}"
        }
        return AdminFiatViewModel(repo) as T
    }
}