package unl.project.distributed.transaction_service.infrastructure.wal;

import java.time.Instant;

/**
 * Registro inmutable del WAL. Cada línea del archivo .wal es un LogEntry serializado.
 *
 * status:
 *   WRITTEN   -> el comando fue escrito y fsync'eado, pero aún no se confirma que
 *                se aplicó/persistió en PostgreSQL (ventana de riesgo ante un crash).
 *   COMMITTED -> se confirmó la aplicación en PostgreSQL; en el recovery se puede
 *                omitir con seguridad (ya está reflejado en la BD).
 */
public record LogEntry(
        long sequence,
        Instant timestamp,
        String transactionId,
        String commandType,   // DEPOSIT | WITHDRAW | TRANSFER
        String payload,       // JSON generado por Command.toWalPayload()
        Status status
) {
    public enum Status { WRITTEN, COMMITTED }

    public String serialize() {
        return String.format("%d|%s|%s|%s|%s|%s",
                sequence, timestamp.toString(), transactionId, commandType, status, payload);
    }

    public static LogEntry deserialize(String line) {
        // formato: seq|timestamp|txId|commandType|status|payloadJson  (el payload puede contener '|', se limita el split)
        String[] parts = line.split("\\|", 6);
        return new LogEntry(
                Long.parseLong(parts[0]),
                Instant.parse(parts[1]),
                parts[2],
                parts[3],
                parts[5],
                Status.valueOf(parts[4])
        );
    }

    public LogEntry withStatus(Status newStatus) {
        return new LogEntry(sequence, timestamp, transactionId, commandType, payload, newStatus);
    }
}
