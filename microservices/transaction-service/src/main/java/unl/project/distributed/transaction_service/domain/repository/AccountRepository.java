package unl.project.distributed.transaction_service.domain.repository;


import java.util.Optional;

import unl.project.distributed.transaction_service.domain.model.Account;

/**
 * PUERTO (hexagonal): el dominio declara el contrato de persistencia
 * sin conocer JPA/PostgreSQL. La implementación vive en infrastructure/persistence.
 */
public interface AccountRepository {
    Optional<Account> findById(String accountId);
    Account save(Account account);

    /** Guarda con control de concurrencia optimista (version). Lanza OptimisticLockException si hay conflicto. */
    Account saveWithVersionCheck(Account account, long expectedVersion);
}
