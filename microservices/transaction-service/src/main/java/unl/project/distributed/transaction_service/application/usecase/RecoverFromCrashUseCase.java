package unl.project.distributed.transaction_service.application.usecase;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import unl.project.distributed.transaction_service.infrastructure.wal.CrashRecoveryEngine;

/**
 * Dispara el crash recovery automáticamente cuando el nodo Spring Boot termina
 * de arrancar (ApplicationReadyEvent), antes de aceptar tráfico de negocio.
 */
@Component
public class RecoverFromCrashUseCase {

    private static final Logger log = LoggerFactory.getLogger(RecoverFromCrashUseCase.class);
    private final CrashRecoveryEngine recoveryEngine;

    public RecoverFromCrashUseCase(CrashRecoveryEngine recoveryEngine) {
        this.recoveryEngine = recoveryEngine;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void onStartup() {
        log.info("Iniciando crash recovery desde WAL...");
        var report = recoveryEngine.recover();
        log.info("Nodo listo para recibir tráfico. Recovery: {} entradas totales, {} reaplicadas, {} omitidas, {} fallidas",
                report.totalEntries(), report.replayed(), report.skippedAlreadyCommitted(), report.failed());
    }
}
