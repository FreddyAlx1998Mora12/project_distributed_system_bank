package unl.project.distributed.transaction_service.domain.repository;

import java.util.Optional;

import unl.project.distributed.transaction_service.domain.model.Transaction;

public interface TransactionRepository {
    Transaction save(Transaction transaction);
    Optional<Transaction> findById(String transactionId);
}
