package unl.project.distributed.transaction_service.domain.command;

import unl.project.distributed.transaction_service.domain.repository.AccountRepository;

/**
 * PATRÓN COMMAND.
 * Cada operación bancaria (depósito, retiro, transferencia) se encapsula como un
 * objeto Command con execute()/undo(). Esto permite:
 *   1) Serializar la operación al WAL de forma uniforme (toWalPayload()).
 *   2) Reconstruirla desde el WAL durante el crash recovery (misma clase, mismos datos).
 *   3) Deshacerla (compensación) si algo falla después de aplicarla parcialmente.
 */
public interface Command {

    /** Aplica la operación sobre las cuentas usando el puerto de repositorio. Debe ser idempotente por 'sequence'. */
    void execute(AccountRepository accountRepository, long walSequence);

    /** Revierte la operación (usado en compensación de transferencias parcialmente aplicadas). */
    void undo(AccountRepository accountRepository);

    /** Representación serializable para persistir en el WAL (LogEntry.payload). */
    String toWalPayload();

    String transactionId();
}
