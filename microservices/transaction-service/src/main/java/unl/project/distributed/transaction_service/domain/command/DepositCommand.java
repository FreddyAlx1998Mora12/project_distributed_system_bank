package unl.project.distributed.transaction_service.domain.command;

import java.math.BigDecimal;

import unl.project.distributed.transaction_service.domain.model.Account;
import unl.project.distributed.transaction_service.domain.repository.AccountRepository;

public class DepositCommand implements Command {

    private final String transactionId;
    private final String toAccountId;
    private final BigDecimal amount;

    public DepositCommand(String transactionId, String toAccountId, BigDecimal amount) {
        this.transactionId = transactionId;
        this.toAccountId = toAccountId;
        this.amount = amount;
    }

    @Override
    public void execute(AccountRepository accountRepository, long walSequence) {
        Account account = accountRepository.findById(toAccountId)
                .orElseThrow(() -> new IllegalStateException("Cuenta no encontrada: " + toAccountId));

        if (account.alreadyApplied(walSequence)) {
            return; // idempotencia: ya se aplicó este seq (replay de recovery)
        }
        account.credit(amount);
        account.markSequenceApplied(walSequence);
        accountRepository.saveWithVersionCheck(account, account.getVersion() - 1);
    }

    @Override
    public void undo(AccountRepository accountRepository) {
        Account account = accountRepository.findById(toAccountId)
                .orElseThrow(() -> new IllegalStateException("Cuenta no encontrada: " + toAccountId));
        account.debit(amount);
        accountRepository.save(account);
    }

    @Override
    public String toWalPayload() {
        return String.format("{\"cmd\":\"DEPOSIT\",\"txId\":\"%s\",\"to\":\"%s\",\"amount\":%s}",
                transactionId, toAccountId, amount.toPlainString());
    }

    @Override
    public String transactionId() { return transactionId; }
}
