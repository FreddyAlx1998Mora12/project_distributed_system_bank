package unl.project.distributed.load_balancer_service.infrastructure.adapter.outbound.monitor;

import java.util.Collections;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import unl.project.distributed.load_balancer_service.domain.model.ClusterTopology;
import unl.project.distributed.load_balancer_service.domain.port.outbound.ClusterMonitorPort;

@Component
public class QuorumMonitorAdapter implements ClusterMonitorPort {

    private static final Logger log = LoggerFactory.getLogger(QuorumMonitorAdapter.class);

    private final RestClient restClient;

    @Value("${monitor.url:http://monitor:8500}")
    private String monitorUrl;

    public QuorumMonitorAdapter(RestClient restClient) {
        this.restClient = restClient;
    }

    @Override
    public ClusterTopology getTopology() {
        try {
            String uri = monitorUrl + "/cluster/topology";
            log.info("Consultando topología del clúster a: {}", uri);
            return restClient.get()
                    .uri(uri)
                    .retrieve()
                    .body(ClusterTopology.class);
        } catch (Exception ex) {
            log.error("Fallo al comunicarse con Quorum Monitor ({}): {}", monitorUrl, ex.getMessage());
            // Si el monitor falla, se asume que no hay quorum para proteger la consistencia
            return new ClusterTopology(null, false, 0, 0, Collections.emptyList());
        }
    }
}
