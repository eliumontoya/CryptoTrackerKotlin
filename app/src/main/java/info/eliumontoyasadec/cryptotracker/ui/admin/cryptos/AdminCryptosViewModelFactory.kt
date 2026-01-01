package info.eliumontoyasadec.cryptotracker.ui.admin.cryptos

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import info.eliumontoyasadec.cryptotracker.domain.repositories.CryptoRepository

class AdminCryptosViewModelFactory(
    private val cryptoRepository: CryptoRepository
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(AdminCryptosViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return AdminCryptosViewModel(cryptoRepository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}