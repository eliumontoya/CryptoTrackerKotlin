package info.eliumontoyasadec.cryptotracker.ui.admin.cryptos

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import info.eliumontoyasadec.cryptotracker.domain.interactor.crypto.DeleteCryptoUseCase
import info.eliumontoyasadec.cryptotracker.domain.interactor.crypto.GetAllCryptosUseCase
import info.eliumontoyasadec.cryptotracker.domain.interactor.crypto.UpsertCryptoUseCase
import info.eliumontoyasadec.cryptotracker.domain.repositories.CryptoRepository

class AdminCryptosViewModelFactory(
    private val cryptoRepository: CryptoRepository
) : ViewModelProvider.Factory {

    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(AdminCryptosViewModel::class.java)) {
            val getAll = GetAllCryptosUseCase(cryptoRepository)
            val upsert = UpsertCryptoUseCase(cryptoRepository)
            val delete = DeleteCryptoUseCase(cryptoRepository)

            @Suppress("UNCHECKED_CAST")
            return AdminCryptosViewModel(
                getAllCryptos = getAll,
                upsertCrypto = upsert,
                deleteCrypto = delete
            ) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}