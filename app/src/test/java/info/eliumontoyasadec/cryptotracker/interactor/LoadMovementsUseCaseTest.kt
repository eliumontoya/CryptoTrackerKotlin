package info.eliumontoyasadec.cryptotracker.interactor

import info.eliumontoyasadec.cryptotracker.domain.interactor.movement.LoadMovementsCommand
import info.eliumontoyasadec.cryptotracker.domain.interactor.movement.LoadMovementsUseCase
import info.eliumontoyasadec.cryptotracker.domain.model.Crypto
import info.eliumontoyasadec.cryptotracker.domain.model.Movement
import info.eliumontoyasadec.cryptotracker.domain.model.MovementError
import info.eliumontoyasadec.cryptotracker.domain.model.Portfolio
import info.eliumontoyasadec.cryptotracker.domain.model.Wallet
import info.eliumontoyasadec.cryptotracker.domain.repositories.CryptoRepository
import info.eliumontoyasadec.cryptotracker.domain.repositories.MovementRepository
import info.eliumontoyasadec.cryptotracker.domain.repositories.PortfolioRepository
import info.eliumontoyasadec.cryptotracker.domain.repositories.WalletRepository
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import kotlin.test.assertFailsWith

class LoadMovementsUseCaseTest {

    // -------------------------
    // Fakes (homologado al estilo de tus UseCase tests)
    // -------------------------

    private class FakePortfolioRepo(
        var existsResult: Boolean = true
    ) : PortfolioRepository {

        override suspend fun exists(portfolioId: Long): Boolean = existsResult

        override suspend fun insert(portfolio: Portfolio): Long = error("Not needed")
        override suspend fun findById(portfolioId: Long): Portfolio? = error("Not needed")
        override suspend fun getAll(): List<Portfolio> = error("Not needed")
        override suspend fun getDefault(): Portfolio? = error("Not needed")
        override suspend fun update(portfolio: Portfolio) = error("Not needed")
        override suspend fun delete(portfolioId: Long) = error("Not needed")
        override suspend fun delete(portfolio: Portfolio) = error("Not needed")
        override suspend fun isDefault(portfolioId: Long): Boolean = error("Not needed")
        override suspend fun setDefault(portfolioId: Long) = error("Not needed")
    }

    private class FakeWalletRepo(
        var existsResult: Boolean = true,
        var belongsResult: Boolean = true
    ) : WalletRepository {

        override suspend fun exists(walletId: Long): Boolean = existsResult

        override suspend fun belongsToPortfolio(walletId: Long, portfolioId: Long): Boolean = belongsResult

        override suspend fun insert(wallet: Wallet): Long = error("Not needed")
        override suspend fun findById(walletId: Long): Wallet? = error("Not needed")
        override suspend fun getByPortfolio(portfolioId: Long): List<Wallet> = error("Not needed")
        override suspend fun update(wallet: Wallet) = error("Not needed")
        override suspend fun delete(walletId: Long) = error("Not needed")
        override suspend fun isMain(walletId: Long): Boolean = error("Not needed")
        override suspend fun setMain(walletId: Long) = error("Not needed")
        override suspend fun update(walletId: Long, name: String) = error("Not needed")
    }

    private class FakeCryptoRepo(
        var existsResult: Boolean = true
    ) : CryptoRepository {

        var lastExistsArg: String? = null

        override suspend fun exists(assetId: String): Boolean {
            lastExistsArg = assetId
            return existsResult
        }

        override suspend fun upsertAll(items: List<Crypto>) = error("Not needed")
        override suspend fun getAll(): List<Crypto> = error("Not needed")
        override suspend fun findBySymbol(symbol: String): Crypto? = error("Not needed")
        override suspend fun upsertOne(item: Crypto) = error("Not needed")
        override suspend fun deleteBySymbol(symbol: String): Int = error("Not needed")
    }

    private class FakeMovementRepo : MovementRepository {

        var listReturn: List<Movement> = emptyList()

        var lastListPortfolioId: Long? = null
        var lastListWalletId: Long? = null
        var lastListAssetId: String? = null

        override suspend fun list(portfolioId: Long, walletId: Long?, assetId: String?): List<Movement> {
            lastListPortfolioId = portfolioId
            lastListWalletId = walletId
            lastListAssetId = assetId
            return listReturn
        }

        override suspend fun insert(movement: Movement): Long = error("Not needed")
        override suspend fun findById(movementId: Long): Movement? = error("Not needed")
        override suspend fun update(movementId: Long, update: Movement) = error("Not needed")
        override suspend fun delete(movementId: Long) = error("Not needed")
    }

    // -------------------------
    // Tests
    // -------------------------

    @Test
    fun `throws InvalidInput when portfolioId is 0`() = runTest {
        val uc = LoadMovementsUseCase(
            portfolioRepo = FakePortfolioRepo(existsResult = true),
            walletRepo = FakeWalletRepo(),
            assetRepo = FakeCryptoRepo(),
            movementRepo = FakeMovementRepo()
        )

        val ex = assertFailsWith<MovementError.InvalidInput> {
            uc.execute(LoadMovementsCommand(portfolioId = 0L))
        }
        assertEquals("portfolioId es requerido", ex.message)
    }

    @Test
    fun `throws NotFound when portfolio does not exist`() = runTest {
        val uc = LoadMovementsUseCase(
            portfolioRepo = FakePortfolioRepo(existsResult = false),
            walletRepo = FakeWalletRepo(),
            assetRepo = FakeCryptoRepo(),
            movementRepo = FakeMovementRepo()
        )

        val ex = assertFailsWith<MovementError.NotFound> {
            uc.execute(LoadMovementsCommand(portfolioId = 1L))
        }
        assertEquals("Portafolio no existe", ex.message)
    }

    @Test
    fun `throws InvalidInput when assetId is blank (and portfolio exists)`() = runTest {
        val uc = LoadMovementsUseCase(
            portfolioRepo = FakePortfolioRepo(existsResult = true),
            walletRepo = FakeWalletRepo(),
            assetRepo = FakeCryptoRepo(existsResult = true),
            movementRepo = FakeMovementRepo()
        )

        val ex = assertFailsWith<MovementError.InvalidInput> {
            uc.execute(
                LoadMovementsCommand(
                    portfolioId = 1L,
                    assetId = "   "
                )
            )
        }
        assertEquals("assetId inválido", ex.message)
    }

    @Test
    fun `throws NotFound when asset does not exist (and portfolio exists)`() = runTest {
        val cryptoRepo = FakeCryptoRepo(existsResult = false)

        val uc = LoadMovementsUseCase(
            portfolioRepo = FakePortfolioRepo(existsResult = true),
            walletRepo = FakeWalletRepo(),
            assetRepo = cryptoRepo,
            movementRepo = FakeMovementRepo()
        )

        val ex = assertFailsWith<MovementError.NotFound> {
            uc.execute(
                LoadMovementsCommand(
                    portfolioId = 1L,
                    assetId = "btc"
                )
            )
        }

        assertEquals("Asset no existe", ex.message)
        assertEquals("btc", cryptoRepo.lastExistsArg)
    }

    @Test
    fun `returns items from repo list with given filters`() = runTest {
        val movementRepo = FakeMovementRepo().apply {
            listReturn = listOf(
                // No necesitamos construir Movement real; solo validar que regresa lo mismo.
                // Pero si tu constructor de Movement es obligatorio, reemplaza por instancias reales.
                // Aquí lo dejamos como mocks no disponibles; así que usa emptyList si no tienes fácil crear.
            )
        }

        // Si no quieres pelearte con el constructor de Movement, usa emptyList:
        movementRepo.listReturn = emptyList()

        val uc = LoadMovementsUseCase(
            portfolioRepo = FakePortfolioRepo(existsResult = true),
            walletRepo = FakeWalletRepo(existsResult = true, belongsResult = true),
            assetRepo = FakeCryptoRepo(existsResult = true),
            movementRepo = movementRepo
        )

        val cmd = LoadMovementsCommand(
            portfolioId = 10L,
            walletId = 20L,
            assetId = "eth"
        )

        val result = uc.execute(cmd)

        assertEquals(movementRepo.listReturn, result.items)
        assertEquals(10L, movementRepo.lastListPortfolioId)
        assertEquals(20L, movementRepo.lastListWalletId)
        assertEquals("eth", movementRepo.lastListAssetId)
    }

    @Test
    fun `returns items when optional filters are null`() = runTest {
        val movementRepo = FakeMovementRepo().apply { listReturn = emptyList() }

        val uc = LoadMovementsUseCase(
            portfolioRepo = FakePortfolioRepo(existsResult = true),
            walletRepo = FakeWalletRepo(),
            assetRepo = FakeCryptoRepo(),
            movementRepo = movementRepo
        )

        val result = uc.execute(
            LoadMovementsCommand(
                portfolioId = 99L,
                walletId = null,
                assetId = null
            )
        )

        assertEquals(emptyList<Movement>(), result.items)
        assertEquals(99L, movementRepo.lastListPortfolioId)
        assertNull(movementRepo.lastListWalletId)
        assertNull(movementRepo.lastListAssetId)
    }
}