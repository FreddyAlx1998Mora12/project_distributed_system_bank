package unl.project.distributed.transaction_service.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

public interface TransactionJpaRepository extends JpaRepository<TransactionEntity, String> {}
