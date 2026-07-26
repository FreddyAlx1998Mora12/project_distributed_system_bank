package unl.project.distributed.quorum_monitor_service;

import java.time.Instant;
import java.util.List;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

import unl.project.distributed.quorum_monitor_service.domain.model.ClusterTopology;
import unl.project.distributed.quorum_monitor_service.domain.model.HealthStatus;
import unl.project.distributed.quorum_monitor_service.domain.model.NodeStatus;
import unl.project.distributed.quorum_monitor_service.domain.service.QuorumAlgorithm;

// @SpringBootTest
class QuorumAlgorithmTest {

    @Test
    void eligeAlNodoSanoDeMenorPrioridadComoLider() {
        var nodes = List.of(
                status("node-1", HealthStatus.DOWN, 1), // caído: no puede ser líder aunque tenga prioridad 1
                status("node-2", HealthStatus.UP, 2),
                status("node-3", HealthStatus.UP, 3)
        );

        ClusterTopology topology = QuorumAlgorithm.evaluate(nodes);

        assertThat(topology.hasQuorum()).isTrue();       // 2 de 3 sanos >= quorum (2)
        assertThat(topology.leaderId()).isEqualTo("node-2"); // el sano de menor priority
    }

    @Test
    void sinQuorumNoHayLiderAunqueUnoEsteSano() {
        var nodes = List.of(
                status("node-1", HealthStatus.DOWN, 1),
                status("node-2", HealthStatus.DOWN, 2),
                status("node-3", HealthStatus.UP, 3)
        );

        ClusterTopology topology = QuorumAlgorithm.evaluate(nodes);

        assertThat(topology.hasQuorum()).isFalse();  // solo 1 de 3 -> no alcanza (2)
        assertThat(topology.leaderId()).isNull();    // split-brain evitado: nadie asume liderazgo sin mayoría
    }

    private NodeStatus status(String id, HealthStatus health, int priority) {
        return new NodeStatus(id, "http://x", priority, health, 0, 0, 10, Instant.now());
    }
}
