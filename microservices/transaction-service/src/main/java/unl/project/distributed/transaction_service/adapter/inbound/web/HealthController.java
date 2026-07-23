package unl.project.distributed.transaction_service.adapter.inbound.web;


import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
public class HealthController {

    @Value("${node.id}")
    private String nodeId;

    @Value("${node.role:REPLICA}")
    private String nodeRole;

    @GetMapping("/health")
    public Map<String, String> health() {
        return Map.of("nodeId", nodeId, "role", nodeRole, "status", "UP");
    }
}
