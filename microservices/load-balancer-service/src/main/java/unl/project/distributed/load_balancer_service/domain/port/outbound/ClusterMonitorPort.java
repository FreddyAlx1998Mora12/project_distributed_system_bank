package unl.project.distributed.load_balancer_service.domain.port.outbound;

import unl.project.distributed.load_balancer_service.domain.model.ClusterTopology;

public interface ClusterMonitorPort {
    ClusterTopology getTopology();
}
