package info.eliumontoyasadec.cryptotracker.ui.admin.delete

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import info.eliumontoyasadec.cryptotracker.data.seed.DatabaseWiper

class DeleteDataViewModelFactory(
    private val wiper: DatabaseWiper
) : ViewModelProvider.Factory {

    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(DeleteDataViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return DeleteDataViewModel(wiper) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}