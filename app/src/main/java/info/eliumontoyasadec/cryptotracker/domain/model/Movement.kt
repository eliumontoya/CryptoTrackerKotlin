package info.eliumontoyasadec.cryptotracker.domain.model

data class Movement(
    val id: String,
    val portfolioId: String,
    val walletId: String,
    val assetId: String,
    val type: MovementType,
    val quantity: Double,
    val price: Double?,
    val feeQuantity: Double,
    val timestamp: Long,
    val notes: String?,
    val groupId: String? = null,

    ){
    /** Equivalente a MovementToInsert(...) */
    constructor(
        portfolioId: String,
        walletId: String,
        assetId: String,
        type: MovementType,
        quantity: Double,
        price: Double?,
        feeQuantity: Double,
        timestamp: Long,
        notes: String?,
        groupId: String? = null
    ) : this(
        id = "",
        groupId = groupId,
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
        notes: String?,
        groupId: String? = null
    ) : this(
        id = "",
        groupId = groupId,

        portfolioId = "",
        walletId = "",
        assetId = "",
        type = type,
        quantity = quantity,
        price = price,
        feeQuantity = feeQuantity,
        timestamp = timestamp,
        notes = notes
    )
}