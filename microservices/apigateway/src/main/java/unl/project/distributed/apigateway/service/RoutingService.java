package unl.project.distributed.apigateway.service;

import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;

@Service
public class RoutingService {

    private static final Logger log = LoggerFactory.getLogger(RoutingService.class);

    private final RestClient restClient;

    @Value("${loadbalancer.url:http://load-balancer:9000}")
    private String loadBalancerUrl;

    public RoutingService(RestClient restClient) {
        this.restClient = restClient;
    }

    @CircuitBreaker(name = "lb-calls", fallbackMethod = "routeFallback")
    public Map<String, Object> route(String operation, Map<String, Object> body) {
        log.info("Gateway → Load Balancer [CB: lb-calls]");
        log.info("Levantando APIGateway, y Erutando operacion {} al LoadBalancer", operation);
        
        // Al no haber try-catch, si el Load Balancer cae, restClient lanza una
        // excepción.
        // Resilience4j la detecta, incrementa el contador de fallos y ejecuta
        // routeFallback.
        return restClient.post()
                .uri(loadBalancerUrl + "/route/" + operation)
                .body(body)
                .retrieve()
                .body(Map.class);
    }

    // Fallback cuando el Load Balancer no responde, fijense en la notacion @cricuitBreaker
    public Map<String, Object> routeFallback(String operation,
            Map<String, Object> body,
            Throwable t) {
        log.error("Circuit Breaker [lb-calls] ACTIVO/ABIERTO para la operacion {}. Causa: {}",operation, t.getMessage());
        return Map.of(
                "status", "ERROR",
                "message", "Sistema temporalmente no disponible. Intente más tarde.");
    }
}
