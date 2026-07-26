package unl.project.distributed.load_balancer_service.domain.model;

import java.util.List;

public record ClusterTopology(
        String leaderId,
        boolean hasQuorum,
        int quorumRequired,
        int totalNodes,
        List<NodeStatus> nodes
) {}
