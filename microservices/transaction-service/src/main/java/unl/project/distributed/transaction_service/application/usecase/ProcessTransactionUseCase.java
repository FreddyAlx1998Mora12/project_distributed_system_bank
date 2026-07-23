package unl.project.distributed.transaction_service.application.usecase;


import org.springframework.stereotype.Service;

import unl.project.distributed.transaction_service.application.dto.TransactionRequest;
import unl.project.distributed.transaction_service.application.dto.TransactionResponse;
import unl.project.distributed.transaction_service.domain.command.Command;
import unl.project.distributed.transaction_service.domain.command.DepositCommand;
import unl.project.distributed.transaction_service.domain.command.TransferCommand;
import unl.project.distributed.transaction_service.domain.command.WithdrawCommand;
import unl.project.distributed.transaction_service.domain.model.Transaction;
import unl.project.distributed.transaction_service.domain.repository.AccountRepository;
import unl.project.distributed.transaction_service.domain.repository.TransactionRepository;
import unl.project.distributed.transaction_service.infrastructure.wal.WriteAheadLog;

/**
 * Orquesta el protocolo Write-Ahead End-to-end:
 *   1. WAL.writeAhead(command)   -> fsync antes de tocar la BD (durabilidad garantizada)
 *   2. command.execute(...)      -> aplica el cambio sobre las cuentas (vía puerto AccountRepository)
 *   3. WAL.markCommitted(...)    -> confirma en el log que ya quedó reflejado en la BD
 *
 * Si el proceso muere entre 1 y 3, el CrashRecoveryEngine reaplicará el comando al reiniciar.
 */
@Service
public class ProcessTransactionUseCase {

    private final WriteAheadLog wal;
    private final AccountRepository accountRepository;
    private final TransactionRepository transactionRepository;

    public ProcessTransactionUseCase(WriteAheadLog wal, AccountRepository accountRepository,
                                      TransactionRepository transactionRepository) {
        this.wal = wal;
        this.accountRepository = accountRepository;
        this.transactionRepository = transactionRepository;
    }

    public TransactionResponse deposit(TransactionRequest req) {
        Transaction tx = Transaction.deposit(req.toAccountId(), req.amount());
        Command cmd = new DepositCommand(tx.getTransactionId(), req.toAccountId(), req.amount());
        return process(tx, cmd);
    }

    public TransactionResponse withdraw(TransactionRequest req) {
        Transaction tx = Transaction.withdraw(req.fromAccountId(), req.amount());
        Command cmd = new WithdrawCommand(tx.getTransactionId(), req.fromAccountId(), req.amount());
        return process(tx, cmd);
    }

    public TransactionResponse transfer(TransactionRequest req) {
        Transaction tx = Transaction.transfer(req.fromAccountId(), req.toAccountId(), req.amount());
        Command cmd = new TransferCommand(tx.getTransactionId(), req.fromAccountId(), req.toAccountId(), req.amount());
        return process(tx, cmd);
    }

    private TransactionResponse process(Transaction tx, Command cmd) {
        transactionRepository.save(tx);

        long seq = wal.writeAhead(cmd); // <-- WRITE-AHEAD: primero el log, siempre

        try {
            cmd.execute(accountRepository, seq);
            wal.markCommitted(seq, cmd);
            tx.markCommitted();
            transactionRepository.save(tx);
            return new TransactionResponse(tx.getTransactionId(), "COMMITTED", "Transacción aplicada correctamente");
        } catch (RuntimeException ex) {
            tx.markFailed();
            transactionRepository.save(tx);
            // Nota: no se marca COMMITTED en el WAL -> el recovery NO reaplicará una operación fallida
            // porque commandFactory reconstruiría el mismo resultado fallido (ej. fondos insuficientes es determinista).
            return new TransactionResponse(tx.getTransactionId(), "FAILED", ex.getMessage());
        }
    }
}
