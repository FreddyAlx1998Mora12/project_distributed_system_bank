package unl.project.distributed.transaction_service.infrastructure.wal;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import unl.project.distributed.transaction_service.domain.command.Command;
import unl.project.distributed.transaction_service.domain.repository.AccountRepository;

/**
 * ALGORITMO DE CRASH RECOVERY.
 *
 * Al reiniciar el nodo, lee secuencialmente el WAL completo y reconstruye
 * el último estado consistente de las cuentas:
 *
 *  1) Recorre las entradas en orden de 'sequence' (el WAL es append-only,
 *     por lo que el orden de escritura = orden de recuperación).
 *  2) Para cada transactionId, determina si llegó a estado COMMITTED.
 *     - Si hay un registro COMMITTED -> la operación ya fue aplicada y
 *       persistida en PostgreSQL antes del crash; se omite (evita doble aplicación).
 *     - Si solo hay WRITTEN (sin su COMMITTED correspondiente) -> la operación
 *       se escribió en el WAL pero el crash ocurrió antes de confirmar su
 *       aplicación en la BD; se REAPLICA (roll-forward / redo).
 *  3) La reaplicación es idempotente gracias a Account.lastAppliedSequence:
 *     si por alguna razón el estado en BD ya reflejaba parcialmente el cambio,
 *     Command.execute() detecta 'alreadyApplied(seq)' y no lo duplica.
 *
 * Este es el mismo principio de "redo log" usado por PostgreSQL/InnoDB.
 */
@Component
public class CrashRecoveryEngine {

    private static final Logger log = LoggerFactory.getLogger(CrashRecoveryEngine.class);

    private final WriteAheadLog wal;
    private final CommandFactory commandFactory;
    private final AccountRepository accountRepository;

    public CrashRecoveryEngine(WriteAheadLog wal, CommandFactory commandFactory,
                                AccountRepository accountRepository) {
        this.wal = wal;
        this.commandFactory = commandFactory;
        this.accountRepository = accountRepository;
    }

    public RecoveryReport recover() {
        List<LogEntry> entries = wal.readAllEntries();
        entries.sort((a, b) -> Long.compare(a.sequence(), b.sequence()));

        Set<String> committedTxIds = new HashSet<>();
        for (LogEntry e : entries) {
            if (e.status() == LogEntry.Status.COMMITTED) committedTxIds.add(e.transactionId());
        }

        int replayed = 0, skipped = 0, failed = 0;

        for (LogEntry entry : entries) {
            if (entry.status() != LogEntry.Status.WRITTEN) continue; // solo interesa redo de intenciones no confirmadas
            if (committedTxIds.contains(entry.transactionId())) {
                skipped++; // ya se confirmó en algún momento -> ya está en la BD
                continue;
            }
            try {
                Command command = commandFactory.fromWalEntry(entry);
                command.execute(accountRepository, entry.sequence());
                replayed++;
                log.info("Recovery: reaplicada transacción {} (seq={})", entry.transactionId(), entry.sequence());
            } catch (Exception ex) {
                failed++;
                log.error("Recovery: fallo al reaplicar transacción {} (seq={}): {}",
                        entry.transactionId(), entry.sequence(), ex.getMessage());
            }
        }

        RecoveryReport report = new RecoveryReport(entries.size(), replayed, skipped, failed);
        log.info("Crash recovery completado: {}", report);
        return report;
    }

    public record RecoveryReport(int totalEntries, int replayed, int skippedAlreadyCommitted, int failed) {}
}
