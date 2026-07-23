package unl.project.distributed.transaction_service.infrastructure.persistence;


import java.math.BigDecimal;

import jakarta.persistence.*;

@Entity
@Table(name = "accounts")
public class AccountEntity {

    @Id
    @Column(name = "account_id")
    private String accountId;

    @Column(name = "account_number", nullable = false, unique = true)
    private String accountNumber;

    @Column(nullable = false, precision = 19, scale = 4)
    private BigDecimal balance;

    @Version
    @Column(nullable = false)
    private long version;

    @Column(name = "last_applied_sequence", nullable = false)
    private long lastAppliedSequence;

    protected AccountEntity() {}

    public AccountEntity(String accountId, String accountNumber, BigDecimal balance,
                          long version, long lastAppliedSequence) {
        this.accountId = accountId;
        this.accountNumber = accountNumber;
        this.balance = balance;
        this.version = version;
        this.lastAppliedSequence = lastAppliedSequence;
    }

    public String getAccountId() { return accountId; }
    public String getAccountNumber() { return accountNumber; }
    public BigDecimal getBalance() { return balance; }
    public long getVersion() { return version; }
    public long getLastAppliedSequence() { return lastAppliedSequence; }

    public void setBalance(BigDecimal balance) { this.balance = balance; }
    public void setLastAppliedSequence(long seq) { this.lastAppliedSequence = seq; }
}
