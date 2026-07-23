package unl.project.distributed.transaction_service.domain.model;

import java.math.BigDecimal;
import java.util.Objects;

/**
 * Entidad de dominio pura (sin anotaciones de framework) que representa
 * una cuenta bancaria. La persistencia se resuelve en infrastructure/persistence
 * mediante un mapper hacia la entidad JPA, manteniendo el dominio libre de
 * dependencias externas (regla de arquitectura hexagonal).
 */
public class Account {

    private final String accountId;
    private String accountNumber;
    private BigDecimal balance;
    private long version;              // optimistic locking (evita lost updates entre réplicas)
    private long lastAppliedSequence;  // último seq del WAL aplicado -> clave del crash recovery idempotente

    public Account(String accountId, String accountNumber, BigDecimal balance,
                    long version, long lastAppliedSequence) {
        this.accountId = accountId;
        this.accountNumber = accountNumber;
        this.balance = balance;
        this.version = version;
        this.lastAppliedSequence = lastAppliedSequence;
    }

    public void credit(BigDecimal amount) {
        if (amount.signum() <= 0) throw new IllegalArgumentException("El monto debe ser positivo");
        this.balance = this.balance.add(amount);
        this.version++;
    }

    public void debit(BigDecimal amount) {
        if (amount.signum() <= 0) throw new IllegalArgumentException("El monto debe ser positivo");
        if (this.balance.compareTo(amount) < 0) {
            throw new InsufficientFundsException(accountId, amount, balance);
        }
        this.balance = this.balance.subtract(amount);
        this.version++;
    }

    public void markSequenceApplied(long sequence) {
        if (sequence <= this.lastAppliedSequence) return; // idempotencia: no reaplicar
        this.lastAppliedSequence = sequence;
    }

    public boolean alreadyApplied(long sequence) {
        return sequence <= this.lastAppliedSequence;
    }

    public String getAccountId() { return accountId; }
    public String getAccountNumber() { return accountNumber; }
    public BigDecimal getBalance() { return balance; }
    public long getVersion() { return version; }
    public long getLastAppliedSequence() { return lastAppliedSequence; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Account)) return false;
        return Objects.equals(accountId, ((Account) o).accountId);
    }

    @Override
    public int hashCode() { return Objects.hash(accountId); }

    public static class InsufficientFundsException extends RuntimeException {
        public InsufficientFundsException(String accountId, BigDecimal requested, BigDecimal available) {
            super(String.format("Fondos insuficientes en cuenta %s: solicitado=%s disponible=%s",
                    accountId, requested, available));
        }
    }
}
