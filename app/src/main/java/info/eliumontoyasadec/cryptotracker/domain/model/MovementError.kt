package info.eliumontoyasadec.cryptotracker.domain.model
sealed class MovementError(message: String) : RuntimeException(message) {
    class InvalidInput(msg: String) : MovementError(msg)
    class NotFound(msg: String) : MovementError(msg)
    class NotAllowed(msg: String) : MovementError(msg)
    class InsufficientHoldings(msg: String) : MovementError(msg)
}