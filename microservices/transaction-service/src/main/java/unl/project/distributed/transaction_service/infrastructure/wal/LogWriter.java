package unl.project.distributed.transaction_service.infrastructure.wal;

import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.channels.FileChannel;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Escritor append-only del WAL con fsync obligatorio (durabilidad).
 * Es la garantía de "Write-Ahead": ningún commit en BD ocurre sin que
 * esto haya devuelto exitosamente primero.
 */
public class LogWriter {

    private final Path walFile;
    private final ReentrantLock writeLock = new ReentrantLock();
    private final AtomicLong sequenceGenerator = new AtomicLong(0);

    public LogWriter(Path walFile) {
        this.walFile = walFile;
    }

    public long nextSequence() {
        return sequenceGenerator.incrementAndGet();
    }

    public void restoreSequenceCounter(long lastKnownSequence) {
        sequenceGenerator.set(Math.max(sequenceGenerator.get(), lastKnownSequence));
    }

    /** Escribe una línea y fuerza el flush a disco (fsync) antes de retornar. Bloqueante y thread-safe. */
    public void append(LogEntry entry) {
        writeLock.lock();
        try (RandomAccessFile raf = new RandomAccessFile(walFile.toFile(), "rw");
             FileChannel channel = raf.getChannel()) {
            channel.position(channel.size()); // append al final
            byte[] bytes = (entry.serialize() + System.lineSeparator()).getBytes(StandardCharsets.UTF_8);
            channel.write(java.nio.ByteBuffer.wrap(bytes));
            channel.force(true); // fsync: garantiza durabilidad ante crash del proceso o del SO
        } catch (IOException e) {
            throw new WalWriteException("No se pudo escribir en el WAL: " + walFile, e);
        } finally {
            writeLock.unlock();
        }
    }

    public static class WalWriteException extends RuntimeException {
        public WalWriteException(String message, Throwable cause) { super(message, cause); }
    }
}
