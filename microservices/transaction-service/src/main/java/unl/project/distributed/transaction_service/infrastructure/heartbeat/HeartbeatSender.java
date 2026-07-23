package unl.project.distributed.transaction_service.infrastructure.heartbeat;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.Map;

/**
 * Cada nodo de transacción envía heartbeats periódicos a la Máquina 5 (Monitor)
 * para que el QuorumManager sepa que sigue vivo y pueda mantener el cluster consistente.
 */
@Component
public class HeartbeatSender {

    private static final Logger log = LoggerFactory.getLogger(HeartbeatSender.class);

    private final RestClient restClient = RestClient.create();

    @Value("${node.id}")
    private String nodeId;

    @Value("${node.role:REPLICA}")
    private String nodeRole;

    @Value("${monitor.url:http://monitor:8500}")
    private String monitorUrl;

    @Scheduled(fixedRateString = "${heartbeat.interval-ms:2000}")
    public void sendHeartbeat() {
        try {
            restClient.post()
                    .uri(monitorUrl + "/heartbeat")
                    .body(Map.of("nodeId", nodeId, "role", nodeRole, "status", "UP"))
                    .retrieve()
                    .toBodilessEntity();
        } catch (Exception ex) {
            log.warn("No se pudo enviar heartbeat al monitor ({}): {}", monitorUrl, ex.getMessage());
        }
    }
}
