package info.eliumontoyasadec.cryptotracker.ui.admin.setup

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider

class SetupInitialViewModelFactory(
    private val ops: SetupInitialOps
) : ViewModelProvider.Factory {

    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(SetupInitialViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return SetupInitialViewModel(ops) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}