package unl.project.distributed.quorum_monitor_service.domain.service;

import java.util.Comparator;
import java.util.List;

import unl.project.distributed.quorum_monitor_service.domain.model.ClusterTopology;
import unl.project.distributed.quorum_monitor_service.domain.model.HealthStatus;
import unl.project.distributed.quorum_monitor_service.domain.model.NodeStatus;

/**
 * Lógica pura de quorum + elección de líder (Bully simplificado por prioridad).
 * CERO dependencias de Spring/framework -> testeable con JUnit puro, sin
 * contexto.
 *
 * Reglas:
 * - quorum = mayoría simple de nodos configurados (N/2 + 1).
 * - Si hay quorum, el líder es el nodo SANO con menor valor de 'priority'.
 * - Si no hay quorum, no hay líder (null) -> el sistema no acepta escrituras
 * seguras, previniendo split-brain (nunca se "adivina" un líder sin mayoría).
 */
public class QuorumAlgorithm {

    private QuorumAlgorithm() {}

    public static ClusterTopology evaluate(List<NodeStatus> nodes) {
        int total = nodes.size();
        int quorumRequired = (total / 2) + 1;

        List<NodeStatus> healthyNodes = nodes.stream()
                .filter(n -> n.health() == HealthStatus.UP)
                .toList();

        boolean hasQuorum = healthyNodes.size() >= quorumRequired;

        String leaderId = hasQuorum
                ? healthyNodes.stream()
                    .min(Comparator.comparingInt(NodeStatus::priority))
                    .map(NodeStatus::nodeId)
                    .orElse(null)
                : null;

        return new ClusterTopology(leaderId, hasQuorum, quorumRequired, total, List.copyOf(nodes));
    }
}
