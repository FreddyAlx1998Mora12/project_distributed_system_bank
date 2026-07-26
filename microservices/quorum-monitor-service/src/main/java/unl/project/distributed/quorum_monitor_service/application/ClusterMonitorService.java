package unl.project.distributed.quorum_monitor_service.application;

import java.util.Map;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import unl.project.distributed.quorum_monitor_service.domain.model.ClusterTopology;
import unl.project.distributed.quorum_monitor_service.domain.model.HealthStatus;
import unl.project.distributed.quorum_monitor_service.domain.model.NodeStatus;
import unl.project.distributed.quorum_monitor_service.domain.service.QuorumAlgorithm;
import unl.project.distributed.quorum_monitor_service.infrastructure.health.NodeHealthRegistry;

/**
 * Orquesta el cálculo de topología y es responsable de los LOGS estructurados
 * de eventos del clúster: QUORUM_LOST/RESTORED, LEADER_ELECTED,
 * NODE_DOWN/RECOVERED.
 * No contiene lógica de negocio bancaria; su única salida es la verdad del
 * clúster.
 */
@Service
public class ClusterMonitorService {

    private static final Logger log = LoggerFactory.getLogger(ClusterMonitorService.class);

    private final NodeHealthRegistry registry;
    private final AtomicReference<ClusterTopology> currentTopology = new AtomicReference<>();

    public ClusterMonitorService(NodeHealthRegistry registry) {
        this.registry = registry;
    }

    public synchronized void recalculate() {
        ClusterTopology previous = currentTopology.get();
        ClusterTopology updated = QuorumAlgorithm.evaluate(registry.snapshot());
        currentTopology.set(updated);
        logTransitions(previous, updated);
    }

    public ClusterTopology currentTopology() {
        ClusterTopology topology = currentTopology.get();
        // Arranque en frío: aún no corrió ningún ciclo de heartbeat.
        return topology != null ? topology : QuorumAlgorithm.evaluate(registry.snapshot());
    }

    private void logTransitions(ClusterTopology previous, ClusterTopology updated) {
        if (previous == null) return; // primer ciclo: nada que comparar todavía

        if (previous.hasQuorum() && !updated.hasQuorum()) {
            log.error("event=QUORUM_LOST aliveNodes={} required={}", countHealthy(updated), updated.quorumRequired());
        } else if (!previous.hasQuorum() && updated.hasQuorum()) {
            log.info("event=QUORUM_RESTORED aliveNodes={} required={}", countHealthy(updated), updated.quorumRequired());
        }

        if (!Objects.equals(previous.leaderId(), updated.leaderId())) {
            log.info("event=LEADER_ELECTED newLeader={} previousLeader={}", updated.leaderId(), previous.leaderId());
        }

        detectNodeTransitions(previous, updated);
    }

    private void detectNodeTransitions(ClusterTopology previous, ClusterTopology updated) {
        Map<String, HealthStatus> previousHealth = previous.nodes().stream()
                .collect(Collectors.toMap(NodeStatus::nodeId, NodeStatus::health));

        for (NodeStatus node : updated.nodes()) {
            HealthStatus before = previousHealth.get(node.nodeId());
            if (before == HealthStatus.UP && node.health() == HealthStatus.DOWN) {
                log.warn("event=NODE_DOWN nodeId={}", node.nodeId());
            } else if (before == HealthStatus.DOWN && node.health() == HealthStatus.UP) {
                log.info("event=NODE_RECOVERED nodeId={}", node.nodeId());
            }
        }
    }

    private long countHealthy(ClusterTopology topology) {
        return topology.nodes().stream().filter(n -> n.health() == HealthStatus.UP).count();
    }
}
