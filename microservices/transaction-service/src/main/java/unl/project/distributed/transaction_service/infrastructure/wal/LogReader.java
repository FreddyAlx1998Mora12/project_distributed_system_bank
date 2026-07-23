package unl.project.distributed.transaction_service.infrastructure.wal;

import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

@Component
public class LogReader {

    /** Lee secuencialmente todo el WAL. Usado al arrancar el nodo para el crash recovery. */
    public List<LogEntry> readAll(Path walFile) {
        List<LogEntry> entries = new ArrayList<>();
        if (!Files.exists(walFile)) {
            return entries;
        }
        try {
            for (String line : Files.readAllLines(walFile)) {
                if (line.isBlank()) continue;
                entries.add(LogEntry.deserialize(line));
            }
        } catch (IOException e) {
            throw new WalReadException("No se pudo leer el WAL: " + walFile, e);
        }
        return entries;
    }

    public static class WalReadException extends RuntimeException {
        public WalReadException(String message, Throwable cause) { super(message, cause); }
    }
}
