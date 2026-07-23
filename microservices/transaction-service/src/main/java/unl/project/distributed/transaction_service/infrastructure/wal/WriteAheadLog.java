package unl.project.distributed.transaction_service.infrastructure.wal;

import java.nio.file.Path;
import java.time.Instant;
import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import unl.project.distributed.transaction_service.domain.command.Command;

/**
 * Fachada del subsistema WAL: coordina LogWriter + LogReader y expone
 * la API que usan los casos de uso de application/.
 */
@Component
public class WriteAheadLog {

    private final Path walFile;
    private final LogWriter writer;
    private final LogReader reader;

    public WriteAheadLog(@Value("${wal.file-path:/data/wal/transactions.wal}") String walPath,
                          LogReader reader) {
        this.walFile = Path.of(walPath);
        this.reader = reader;
        this.writer = new LogWriter(this.walFile);
        // al construir, restauramos el contador de secuencia a partir del último registro existente
        List<LogEntry> existing = reader.readAll(this.walFile);
        long lastSeq = existing.stream().mapToLong(LogEntry::sequence).max().orElse(0);
        this.writer.restoreSequenceCounter(lastSeq);
    }

    /** Paso 1 del protocolo Write-Ahead: persistir la intención ANTES de tocar la BD. */
    public long writeAhead(Command command) {
        long seq = writer.nextSequence();
        LogEntry entry = new LogEntry(seq, Instant.now(), command.transactionId(),
                command.getClass().getSimpleName(), command.toWalPayload(), LogEntry.Status.WRITTEN);
        writer.append(entry);
        return seq;
    }

    /** Paso 2: una vez aplicada y persistida la operación en PostgreSQL, se marca COMMITTED. */
    public void markCommitted(long sequence, Command command) {
        LogEntry committed = new LogEntry(sequence, Instant.now(), command.transactionId(),
                command.getClass().getSimpleName(), command.toWalPayload(), LogEntry.Status.COMMITTED);
        writer.append(committed); // append-only: se agrega un nuevo registro "tombstone" de commit
    }

    public List<LogEntry> readAllEntries() {
        return reader.readAll(walFile);
    }
}
