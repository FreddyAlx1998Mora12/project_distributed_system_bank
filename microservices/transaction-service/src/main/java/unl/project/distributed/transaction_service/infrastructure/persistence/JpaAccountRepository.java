package unl.project.distributed.transaction_service.infrastructure.persistence;

import java.util.Optional;

import org.springframework.stereotype.Repository;

import jakarta.persistence.OptimisticLockException;
import unl.project.distributed.transaction_service.domain.model.Account;
import unl.project.distributed.transaction_service.domain.repository.AccountRepository;

/**
 * ADAPTADOR (hexagonal): implementa el puerto AccountRepository usando Spring Data JPA.
 * Traduce entre el modelo de dominio puro (Account) y la entidad de persistencia (AccountEntity).
 */
@Repository
public class JpaAccountRepository implements AccountRepository {

    private final AccountJpaRepository jpaRepository;

    public JpaAccountRepository(AccountJpaRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    @Override
    public Optional<Account> findById(String accountId) {
        return jpaRepository.findById(accountId).map(this::toDomain);
    }

    @Override
    public Account save(Account account) {
        AccountEntity entity = new AccountEntity(account.getAccountId(), account.getAccountNumber(),
                account.getBalance(), account.getVersion(), account.getLastAppliedSequence());
        return toDomain(jpaRepository.save(entity));
    }

    @Override
    public Account saveWithVersionCheck(Account account, long expectedVersion) {
        AccountEntity current = jpaRepository.findById(account.getAccountId())
                .orElseThrow(() -> new IllegalStateException("Cuenta no encontrada: " + account.getAccountId()));
        if (current.getVersion() != expectedVersion) {
            throw new OptimisticLockException(
                    "Conflicto de concurrencia en cuenta " + account.getAccountId() +
                    " (esperado=" + expectedVersion + ", actual=" + current.getVersion() + ")");
        }
        current.setBalance(account.getBalance());
        current.setLastAppliedSequence(account.getLastAppliedSequence());
        return toDomain(jpaRepository.save(current));
    }

    private Account toDomain(AccountEntity e) {
        return new Account(e.getAccountId(), e.getAccountNumber(), e.getBalance(),
                e.getVersion(), e.getLastAppliedSequence());
    }
}
