package unl.project.distributed.quorum_monitor_service.infrastructure.health;

import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.stereotype.Component;

import unl.project.distributed.quorum_monitor_service.domain.model.NodeStatus;
import unl.project.distributed.quorum_monitor_service.infrastructure.config.ClusterProperties;

/**
 * Almacén thread-safe del estado observado de cada nodo. Es el único lugar
 * donde vive el estado mutable del sistema (todo lo demás son records
 * inmutables).
 */
@Component
public class NodeHealthRegistry {

    private final ConcurrentHashMap<String, NodeStatus> statuses = new ConcurrentHashMap<>();
    private final int failureThreshold;
    private final int recoveryThreshold;

    public NodeHealthRegistry(ClusterProperties properties) {
        this.failureThreshold = properties.healthCheck().failureThreshold();
        this.recoveryThreshold = properties.healthCheck().recoveryThreshold();
        properties.nodes().forEach(node ->
                statuses.put(node.id(), NodeStatus.initial(node.id(), node.url(), node.priority())));
    }

    public void recordSuccess(String nodeId, long latencyMs) {
        statuses.computeIfPresent(nodeId, (id, current) -> current.withSuccess(latencyMs, recoveryThreshold));
    }

    public void recordFailure(String nodeId) {
        statuses.computeIfPresent(nodeId, (id, current) -> current.withFailure(failureThreshold));
    }

    public List<NodeStatus> snapshot() {
        return List.copyOf(statuses.values());
    }
}
