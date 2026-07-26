package unl.project.distributed.quorum_monitor_service.domain.model;

import java.util.List;

/**
 * Veredicto del clúster en un instante dado. Es lo que consume el Load
 * Balancer.
 * leaderId == null significa "no hay líder seguro" (sin quorum) -> el LB no
 * debe
 * enrutar escrituras a nadie hasta que esto cambie.
 */
public record ClusterTopology(
        String leaderId,
        boolean hasQuorum,
        int quorumRequired,
        int totalNodes,
        List<NodeStatus> nodes) {
}

