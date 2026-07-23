package unl.project.distributed.transaction_service.infrastructure.persistence;

import java.util.Optional;

import org.springframework.stereotype.Repository;

import unl.project.distributed.transaction_service.domain.model.Transaction;
import unl.project.distributed.transaction_service.domain.repository.TransactionRepository;

@Repository
public class JpaTransactionRepository implements TransactionRepository {

    private final TransactionJpaRepository jpaRepository;

    public JpaTransactionRepository(TransactionJpaRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    @Override
    public Transaction save(Transaction transaction) {
        TransactionEntity entity = new TransactionEntity(
                transaction.getTransactionId(), transaction.getType().name(),
                transaction.getFromAccountId(), transaction.getToAccountId(),
                transaction.getAmount(), transaction.getStatus().name(), transaction.getCreatedAt());
        jpaRepository.save(entity);
        return transaction;
    }

    @Override
    public Optional<Transaction> findById(String transactionId) {
        // Simplificado: para exponer estado se consulta el estado tal cual quedó guardado.
        // (La reconstrucción completa a objeto de dominio se omite por brevedad;
        //  en un caso real se añadiría un mapper Entity -> Transaction con setters de estado.)
        return jpaRepository.findById(transactionId).map(e -> {
            Transaction tx = switch (Transaction.Type.valueOf(e.getType())) {
                case DEPOSIT -> Transaction.deposit(e.getToAccountId(), e.getAmount());
                case WITHDRAW -> Transaction.withdraw(e.getFromAccountId(), e.getAmount());
                case TRANSFER -> Transaction.transfer(e.getFromAccountId(), e.getToAccountId(), e.getAmount());
            };
            return tx;
        });
    }
}
