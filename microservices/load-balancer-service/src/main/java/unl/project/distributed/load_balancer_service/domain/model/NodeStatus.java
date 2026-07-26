package unl.project.distributed.load_balancer_service.domain.model;

import java.time.Instant;

public record NodeStatus(
        String nodeId,
        String url,
        int priority,
        HealthStatus health,
        int consecutiveFailures,
        int consecutiveSuccesses,
        long lastLatencyMs,
        Instant lastCheckedAt
) {}
