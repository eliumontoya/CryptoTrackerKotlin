package info.eliumontoyasadec.cryptotracker.interactor
import info.eliumontoyasadec.cryptotracker.domain.repositories.CryptoRepository
import info.eliumontoyasadec.cryptotracker.domain.model.Holding
import info.eliumontoyasadec.cryptotracker.domain.repositories.HoldingRepository
import info.eliumontoyasadec.cryptotracker.domain.model.Movement
import info.eliumontoyasadec.cryptotracker.domain.repositories.MovementRepository
import info.eliumontoyasadec.cryptotracker.domain.repositories.PortfolioRepository
import info.eliumontoyasadec.cryptotracker.domain.repositories.TransactionRunner
import info.eliumontoyasadec.cryptotracker.domain.repositories.WalletRepository


class FakeTx : TransactionRunner {
    override suspend fun <T> runInTransaction(block: suspend () -> T): T = block()
}

class FakePortfolioRepo(private val exists: Boolean = true) : PortfolioRepository {
    override suspend fun exists(portfolioId: Long) = exists
}

class FakeWalletRepo(
    private val exists: Boolean = true,
    private val belongs: Boolean = true
) : WalletRepository {
    override suspend fun exists(walletId: Long) = exists
    override suspend fun belongsToPortfolio(walletId: Long, portfolioId: Long) = belongs
}

class FakeCryptoRepo(private val exists: Boolean = true) : CryptoRepository {
    override suspend fun exists(assetId: String) = exists
}


class FakeMovementRepo : MovementRepository {

    /** Base de datos en memoria */
    private val store = mutableMapOf<Long, Movement>()

    /** Hooks de verificación para tests */
    var lastInserted: Movement? = null
        private set

    var lastUpdateId: Long? = null
        private set

    var lastDeletedId: Long? = null
        private set

    /** Utilidad para preparar escenarios desde el test */
    fun seed(movement: Movement) {
        store[movement.id] = movement
    }

    override suspend fun insert(movement: Movement): Long {
        val id = System.currentTimeMillis()

        val movement = Movement(
            id = id,
            portfolioId = movement.portfolioId,
            walletId = movement.walletId,
            assetId = movement.assetId,
            type = movement.type,
            quantity = movement.quantity,
            price = movement.price,
            feeQuantity = movement.feeQuantity,
            timestamp = movement.timestamp,
            notes = movement.notes,
            groupId = movement.groupId
        )

        store[id] = movement
        lastInserted = movement

        return id
    }

    override suspend fun findById(movementId: Long): Movement? {
        return store[movementId]
    }

    override suspend fun update(movementId: Long, update: Movement) {
        val existing = store[movementId] ?: return

        store[movementId] = existing.copy(
            type = update.type ?: existing.type,
            quantity = update.quantity ?: existing.quantity,
            feeQuantity = update.feeQuantity ?: existing.feeQuantity,
            price = update.price ?: existing.price,
            timestamp = update.timestamp ?: existing.timestamp,
            groupId = update.groupId ?: existing.groupId
        )

        lastUpdateId = movementId
    }

    override suspend fun delete(movementId: Long) {
        store.remove(movementId)
        lastDeletedId = movementId
    }
}

class FakeHoldingRepo : HoldingRepository {
    var holding: Holding? = null
    var lastUpsertQty: Double? = null


    override suspend fun findByWalletAsset(walletId: Long, assetId: String): Holding? =
        holding?.takeIf { it.walletId == walletId && it.assetId == assetId }


    override suspend fun upsert(
        portfolioId: Long,
        walletId: Long,
        assetId: String,
        newQuantity: Double,
        updatedAt: Long
    ): Holding {
        lastUpsertQty = newQuantity
        val newHolding = Holding(
            id = holding?.id ?: "hol-001",
            portfolioId = portfolioId,
            walletId = walletId,
            assetId = assetId,
            quantity = newQuantity,
            updatedAt = updatedAt
        )
        holding = newHolding
        return newHolding
    }


}

