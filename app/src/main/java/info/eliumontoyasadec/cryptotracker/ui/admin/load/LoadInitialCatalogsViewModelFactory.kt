package info.eliumontoyasadec.cryptotracker.ui.admin.load

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import info.eliumontoyasadec.cryptotracker.data.seed.CatalogSeeder

class LoadInitialCatalogsViewModelFactory(
    private val seeder: CatalogSeeder
) : ViewModelProvider.Factory {

    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(LoadInitialCatalogsViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return LoadInitialCatalogsViewModel(seeder) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}