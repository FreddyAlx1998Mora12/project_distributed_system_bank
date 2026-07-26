package unl.project.distributed.quorum_monitor_service.domain.model;

import java.time.Instant;

/**
 * Estado observado de un nodo, con HISTÉRESIS para evitar "flapping":
 * un solo fallo de red aislado no tumba un nodo sano, y una sola respuesta
 * exitosa no resucita instantáneamente un nodo caído. Se requieren
 * 'failureThreshold'/'recoveryThreshold' resultados consecutivos en la
 * misma dirección antes de cambiar de estado real.
 *
 * Inmutable a propósito (Java record): cada transición produce una nueva
 * instancia, nunca se muta en sitio -> seguro de compartir entre hilos.
 */

public record NodeStatus(
        String nodeId,
        String url,
        int priority,
        HealthStatus health,
        int consecutiveFailures,
        int consecutiveSuccesses,
        long lastLatencyMs,
        Instant lastCheckedAt
) {
    public static NodeStatus initial(String nodeId, String url, int priority) {
        // Arranca en DOWN: nunca asumimos que un nodo está sano sin haber
        // recibido al menos una respuesta real. Evita falsos "healthy" al bootear.
        return new NodeStatus(nodeId, url, priority, HealthStatus.DOWN, 0, 0, -1, null);
    }

    public NodeStatus withSuccess(long latencyMs, int recoveryThreshold) {
        int successes = consecutiveSuccesses + 1;
        HealthStatus newHealth = (health == HealthStatus.DOWN && successes < recoveryThreshold)
                ? HealthStatus.DOWN
                : HealthStatus.UP;
        return new NodeStatus(nodeId, url, priority, newHealth, 0, successes, latencyMs, Instant.now());
    }

    public NodeStatus withFailure(int failureThreshold) {
        int failures = consecutiveFailures + 1;
        HealthStatus newHealth = (health == HealthStatus.UP && failures < failureThreshold)
                ? HealthStatus.UP
                : HealthStatus.DOWN;
        return new NodeStatus(nodeId, url, priority, newHealth, failures, 0, lastLatencyMs, Instant.now());
    }
}
