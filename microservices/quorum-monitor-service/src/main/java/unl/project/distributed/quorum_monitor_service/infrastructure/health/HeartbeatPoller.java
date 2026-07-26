package unl.project.distributed.quorum_monitor_service.infrastructure.health;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import jakarta.annotation.PreDestroy;
import unl.project.distributed.quorum_monitor_service.application.ClusterMonitorService;
import unl.project.distributed.quorum_monitor_service.domain.model.NodeConfig;
import unl.project.distributed.quorum_monitor_service.infrastructure.config.ClusterProperties;

/**
 * ÚNICO emisor de heartbeats hacia los nodos (heartbeat PROACTIVO, según lo
 * acordado).
 * El Load Balancer NO hace polling propio; consulta la topología que este
 * componente calcula.
 */
@Component
public class HeartbeatPoller {
    
    private static final Logger log = LoggerFactory.getLogger(HeartbeatPoller.class);

    private final ClusterProperties properties;
    private final NodeHealthRegistry registry;
    private final RestClient restClient;
    private final ClusterMonitorService monitorService;
    private final ExecutorService executor;

    public HeartbeatPoller(ClusterProperties properties, NodeHealthRegistry registry,
                            RestClient restClient, ClusterMonitorService monitorService) {
        this.properties = properties;
        this.registry = registry;
        this.restClient = restClient;
        this.monitorService = monitorService;
        // Un hilo por nodo configurado: un nodo lento/colgado no debe robarle
        // tiempo de CPU al chequeo de los demás en el mismo ciclo.
        this.executor = Executors.newFixedThreadPool(Math.max(properties.nodes().size(), 1));
    }

    @Scheduled(fixedRateString = "${cluster.health-check.interval-ms:2000}")
    public void pollAll() {
        List<CompletableFuture<Void>> checks = properties.nodes().stream()
                .map(node -> CompletableFuture.runAsync(() -> checkNode(node), executor))
                .toList();

        // Esperamos a que termine el ciclo completo (acotado por el timeout de
        // cada llamada individual) antes de recalcular el quorum con datos frescos.
        CompletableFuture.allOf(checks.toArray(new CompletableFuture[0])).join();

        monitorService.recalculate();
    }

    private void checkNode(NodeConfig node) {
        long start = System.currentTimeMillis();
        try {
            restClient.get()
                    .uri(node.url() + "/actuator/health")
                    .retrieve()
                    .toBodilessEntity();

            registry.recordSuccess(node.id(), System.currentTimeMillis() - start);

        } catch (RestClientException ex) {
            // Timeout, conexión rechazada, o HTTP >=400 caen todos aquí.
            // Se captura LOCALMENTE: una excepción de este nodo no debe propagarse
            // y tumbar el chequeo (ni el hilo) de los otros nodos del mismo ciclo.
            log.warn("Heartbeat fallido nodeId={} url={} causa={}", node.id(), node.url(), ex.getMessage());
            registry.recordFailure(node.id());
        }
    }

    @PreDestroy
    public void shutdown() {
        executor.shutdownNow();
    }
}
