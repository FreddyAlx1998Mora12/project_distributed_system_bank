package unl.project.distributed.load_balancer_service.infrastructure.adapter.outbound.node;

import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import unl.project.distributed.load_balancer_service.domain.port.outbound.TransactionNodePort;

@Component
public class ResilientTransactionNodeAdapter implements TransactionNodePort {

    private static final Logger log = LoggerFactory.getLogger(ResilientTransactionNodeAdapter.class);

    private final RestClient restClient;
    private final CircuitBreakerRegistry circuitBreakerRegistry;

    public ResilientTransactionNodeAdapter(RestClient restClient, CircuitBreakerRegistry circuitBreakerRegistry) {
        this.restClient = restClient;
        this.circuitBreakerRegistry = circuitBreakerRegistry;
    }

    @Override
    @SuppressWarnings("unchecked")
    public Map<String, Object> executeTransaction(String nodeId, String targetUrl, String operation, Map<String, Object> body) {
        // Obtener o crear Circuit Breaker INDIVIDUAL por nodo
        String cbName = "node-cb-" + nodeId;
        CircuitBreaker cb = circuitBreakerRegistry.circuitBreaker(cbName);

        log.info("Ejecutando llamada a nodo '{}' con Circuit Breaker '{}' [Estado CB: {}]",
                nodeId, cbName, cb.getState());

        return cb.executeSupplier(() -> {
            String uri = targetUrl + "/transactions/" + operation;
            log.info("Forwarding HTTP POST to: {}", uri);
            return restClient.post()
                    .uri(uri)
                    .body(body)
                    .retrieve()
                    .body(Map.class);
        });
    }
}
