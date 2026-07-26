package unl.project.distributed.quorum_monitor_service.adapter.inbound.web;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import unl.project.distributed.quorum_monitor_service.application.ClusterMonitorService;
import unl.project.distributed.quorum_monitor_service.domain.model.ClusterTopology;

@RestController
@RequestMapping("/cluster")
public class TopologyController {
    private final ClusterMonitorService monitorService;

    public TopologyController(ClusterMonitorService monitorService) {
        this.monitorService = monitorService;
    }

    /** Contrato consumido por el Load Balancer: quién es el líder y si hay quorum. */
    @GetMapping("/topology")
    public ClusterTopology topology() {
        return monitorService.currentTopology();
    }
}
