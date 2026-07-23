package unl.project.distributed.transaction_service.domain.command;


import java.math.BigDecimal;

import unl.project.distributed.transaction_service.domain.model.Account;
import unl.project.distributed.transaction_service.domain.repository.AccountRepository;

public class WithdrawCommand implements Command {

    private final String transactionId;
    private final String fromAccountId;
    private final BigDecimal amount;

    public WithdrawCommand(String transactionId, String fromAccountId, BigDecimal amount) {
        this.transactionId = transactionId;
        this.fromAccountId = fromAccountId;
        this.amount = amount;
    }

    @Override
    public void execute(AccountRepository accountRepository, long walSequence) {
        Account account = accountRepository.findById(fromAccountId)
                .orElseThrow(() -> new IllegalStateException("Cuenta no encontrada: " + fromAccountId));

        if (account.alreadyApplied(walSequence)) {
            return;
        }
        account.debit(amount);
        account.markSequenceApplied(walSequence);
        accountRepository.saveWithVersionCheck(account, account.getVersion() - 1);
    }

    @Override
    public void undo(AccountRepository accountRepository) {
        Account account = accountRepository.findById(fromAccountId)
                .orElseThrow(() -> new IllegalStateException("Cuenta no encontrada: " + fromAccountId));
        account.credit(amount);
        accountRepository.save(account);
    }

    @Override
    public String toWalPayload() {
        return String.format("{\"cmd\":\"WITHDRAW\",\"txId\":\"%s\",\"from\":\"%s\",\"amount\":%s}",
                transactionId, fromAccountId, amount.toPlainString());
    }

    @Override
    public String transactionId() { return transactionId; }
}
