package unl.project.distributed.transaction_service.domain.model;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public class Transaction {

    public enum Type { DEPOSIT, WITHDRAW, TRANSFER }

    private final String transactionId;
    private final Type type;
    private final String fromAccountId; // null en DEPOSIT
    private final String toAccountId;   // null en WITHDRAW
    private final BigDecimal amount;
    private final Instant createdAt;
    private TransactionStatus status;

    private Transaction(String transactionId, Type type, String fromAccountId,
                         String toAccountId, BigDecimal amount) {
        this.transactionId = transactionId;
        this.type = type;
        this.fromAccountId = fromAccountId;
        this.toAccountId = toAccountId;
        this.amount = amount;
        this.createdAt = Instant.now();
        this.status = TransactionStatus.PENDING;
    }

    public static Transaction deposit(String toAccountId, BigDecimal amount) {
        return new Transaction(UUID.randomUUID().toString(), Type.DEPOSIT, null, toAccountId, amount);
    }

    public static Transaction withdraw(String fromAccountId, BigDecimal amount) {
        return new Transaction(UUID.randomUUID().toString(), Type.WITHDRAW, fromAccountId, null, amount);
    }

    public static Transaction transfer(String fromAccountId, String toAccountId, BigDecimal amount) {
        return new Transaction(UUID.randomUUID().toString(), Type.TRANSFER, fromAccountId, toAccountId, amount);
    }

    public void markCommitted() { this.status = TransactionStatus.COMMITTED; }
    public void markFailed() { this.status = TransactionStatus.FAILED; }
    public void markRolledBack() { this.status = TransactionStatus.ROLLED_BACK; }

    public String getTransactionId() { return transactionId; }
    public Type getType() { return type; }
    public String getFromAccountId() { return fromAccountId; }
    public String getToAccountId() { return toAccountId; }
    public BigDecimal getAmount() { return amount; }
    public Instant getCreatedAt() { return createdAt; }
    public TransactionStatus getStatus() { return status; }
}
