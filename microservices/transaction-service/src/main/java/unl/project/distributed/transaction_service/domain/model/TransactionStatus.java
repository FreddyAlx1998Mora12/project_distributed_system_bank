package unl.project.distributed.transaction_service.domain.model;

/**
 * Ciclo de vida de una transacción bancaria.
 * PENDING     -> escrita en el WAL, aún no aplicada/confirmada en la BD.
 * COMMITTED   -> aplicada en la BD y confirmada en el WAL.
 * FAILED      -> no pudo aplicarse (ej. fondos insuficientes).
 * ROLLED_BACK -> revertida explícitamente (usada por Command.undo()).
 */
public enum TransactionStatus {
    PENDING,
    COMMITTED,
    FAILED,
    ROLLED_BACK
}
