package unl.project.distributed.quorum_monitor_service.infrastructure.config;

import java.util.List;

import org.springframework.boot.context.properties.ConfigurationProperties;

import unl.project.distributed.quorum_monitor_service.domain.model.NodeConfig;

@ConfigurationProperties(prefix = "cluster")
public record ClusterProperties(
    List<NodeConfig> nodes,
    HealthCheckSettings healthCheck
) {
    public record HealthCheckSettings(
            long intervalMs,
            long timeoutMs,
            int failureThreshold,
            int recoveryThreshold) {
    }
}
