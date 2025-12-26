package info.eliumontoyasadec.cryptotracker.domain.repositories

interface AssetRepository {
    suspend fun exists(assetId: String): Boolean
}