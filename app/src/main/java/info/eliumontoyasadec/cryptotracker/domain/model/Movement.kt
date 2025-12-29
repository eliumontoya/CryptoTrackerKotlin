package info.eliumontoyasadec.cryptotracker.domain.model

data class Movement(
    val id: Long = 0L,
    val portfolioId: Long,
    val walletId: Long,
    val assetId: String,
    val type: MovementType,
    val quantity: Double,
    val price: Double?,
    val feeQuantity: Double,
    val timestamp: Long,
    val notes: String?,
    val groupId: Long?,

    )
{

    /** Equivalente a MovementToInsert(...) */
    constructor(
        id: Long,
        portfolioId: Long,
        walletId: Long,
        assetId: String,
        type: MovementType,
        quantity: Double,
        price: Double?,
        feeQuantity: Double,
        timestamp: Long,
        notes: String,
    ) : this(
        id = id,
        portfolioId = portfolioId,
        walletId = walletId,
        assetId = assetId,
        type = type,
        quantity = quantity,
        price = price,
        feeQuantity = feeQuantity,
        timestamp = timestamp,
        notes = notes,
        groupId = 0L,

        )

    /** Equivalente a MovementToInsert(...) */
    constructor(
        id: Long,
        portfolioId: Long,
        walletId: Long,
        assetId: String,
        type: MovementType,
        quantity: Double,
        price: Double?,
        feeQuantity: Double,
        timestamp: Long,

    ) : this(
        id = id,
        portfolioId = portfolioId,
        walletId = walletId,
        assetId = assetId,
        type = type,
        quantity = quantity,
        price = price,
        feeQuantity = feeQuantity,
        timestamp = timestamp,
        notes = "",
        groupId = 0L,

        )




    /** Equivalente a MovementToInsert(...) */
    constructor(
        portfolioId: Long,
        walletId: Long,
        assetId: String,
        type: MovementType,
        quantity: Double,
        price: Double?,
        feeQuantity: Double,
        timestamp: Long,
        notes: String,
     ) : this(
        id = 0L,
        groupId = 0L,
        portfolioId = portfolioId,
        walletId = walletId,
        assetId = assetId,
        type = type,
        quantity = quantity,
        price = price,
        feeQuantity = feeQuantity,
        timestamp = timestamp,
        notes = notes
    )

    /** Equivalente a MovementUpdate(...). Movement-parche: ids vacíos a propósito. */
    constructor(
        type: MovementType,
        quantity: Double,
        price: Double?,
        feeQuantity: Double,
        timestamp: Long,
        notes: String,
     ) : this(
        id = 0L,
        groupId = 0L,
        portfolioId = 0L,
        walletId = 0L,
        assetId = "",
        type = type,
        quantity = quantity,
        price = price,
        feeQuantity = feeQuantity,
        timestamp = timestamp,
        notes = notes
    )

    /** Equivalente a MovementUpdate(...). Movement-parche: ids vacíos a propósito. */
    constructor(
        type: MovementType,
        quantity: Double,
        price: Double?,
        feeQuantity: Double,
        timestamp: Long,
        notes: String,
        groupId: Long = 0L
    ) : this(
        id = 0L,
        groupId = groupId,
        portfolioId = 0L,
        walletId = 0L,
        assetId = "",
        type = type,
        quantity = quantity,
        price = price,
        feeQuantity = feeQuantity,
        timestamp = timestamp,
        notes = notes
    )
}