package unl.project.distributed.transaction_service.infrastructure.wal;


import java.math.BigDecimal;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.springframework.stereotype.Component;

import unl.project.distributed.transaction_service.domain.command.Command;
import unl.project.distributed.transaction_service.domain.command.DepositCommand;
import unl.project.distributed.transaction_service.domain.command.TransferCommand;
import unl.project.distributed.transaction_service.domain.command.WithdrawCommand;

/**
 * Reconstruye un Command a partir del payload JSON almacenado en el WAL.
 * Parser deliberadamente simple (regex) para no depender de librerías JSON
 * en la ruta crítica de recovery; en producción se recomendaría Jackson.
 */
@Component
public class CommandFactory {

    private static final Pattern FIELD = Pattern.compile("\"(\\w+)\":\"?([^\",}]+)\"?");

    public Command fromWalEntry(LogEntry entry) {
        var fields = new java.util.HashMap<String, String>();
        Matcher m = FIELD.matcher(entry.payload());
        while (m.find()) fields.put(m.group(1), m.group(2));

        String txId = fields.get("txId");
        BigDecimal amount = new BigDecimal(fields.get("amount"));

        return switch (entry.commandType()) {
            case "DepositCommand" -> new DepositCommand(txId, fields.get("to"), amount);
            case "WithdrawCommand" -> new WithdrawCommand(txId, fields.get("from"), amount);
            case "TransferCommand" -> new TransferCommand(txId, fields.get("from"), fields.get("to"), amount);
            default -> throw new IllegalStateException("Comando desconocido en WAL: " + entry.commandType());
        };
    }
}
