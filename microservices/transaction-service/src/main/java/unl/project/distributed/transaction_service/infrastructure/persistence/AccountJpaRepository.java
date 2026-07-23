package unl.project.distributed.transaction_service.infrastructure.persistence;


import org.springframework.data.jpa.repository.JpaRepository;

/** Repositorio técnico de Spring Data; NO es el puerto de dominio (ese es AccountRepository). */
public interface AccountJpaRepository extends JpaRepository<AccountEntity, String> {}
