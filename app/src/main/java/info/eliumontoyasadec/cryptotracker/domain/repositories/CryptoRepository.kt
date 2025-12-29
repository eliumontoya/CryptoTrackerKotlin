package info.eliumontoyasadec.cryptotracker.domain.repositories

interface CryptoRepository {
    suspend fun exists(assetId: String): Boolean
}