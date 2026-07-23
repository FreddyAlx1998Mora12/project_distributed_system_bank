package unl.project.distributed.transaction_service.domain.command;

import java.math.BigDecimal;

import unl.project.distributed.transaction_service.domain.repository.AccountRepository;

/**
 * Compone Withdraw + Deposit (Composite ligero sobre Command).
 * Si el crédito falla tras el débito, se compensa con undo() del débito
 * -> mantiene la propiedad ACID de atomicidad a nivel de aplicación.
 */
public class TransferCommand implements Command {

    private final String transactionId;
    private final String fromAccountId;
    private final String toAccountId;
    private final BigDecimal amount;
    private final WithdrawCommand withdraw;
    private final DepositCommand deposit;

    public TransferCommand(String transactionId, String fromAccountId, String toAccountId, BigDecimal amount) {
        this.transactionId = transactionId;
        this.fromAccountId = fromAccountId;
        this.toAccountId = toAccountId;
        this.amount = amount;
        this.withdraw = new WithdrawCommand(transactionId, fromAccountId, amount);
        this.deposit = new DepositCommand(transactionId, toAccountId, amount);
    }

    @Override
    public void execute(AccountRepository accountRepository, long walSequence) {
        withdraw.execute(accountRepository, walSequence);
        try {
            deposit.execute(accountRepository, walSequence);
        } catch (RuntimeException ex) {
            withdraw.undo(accountRepository); // compensación
            throw ex;
        }
    }

    @Override
    public void undo(AccountRepository accountRepository) {
        deposit.undo(accountRepository);
        withdraw.undo(accountRepository);
    }

    @Override
    public String toWalPayload() {
        return String.format("{\"cmd\":\"TRANSFER\",\"txId\":\"%s\",\"from\":\"%s\",\"to\":\"%s\",\"amount\":%s}",
                transactionId, fromAccountId, toAccountId, amount.toPlainString());
    }

    @Override
    public String transactionId() { return transactionId; }
}
