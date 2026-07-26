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
        
        try {
            Map<String, Object> response = restClient.post()
                    .uri(loadBalancerUrl + "/route/" + operation)
                    .body(body)
                    .retrieve()
                    .body(Map.class);
            
            log.info("Respuesta exitosa del Load Balancer para operación: {}", operation);
            return response;
            
        } catch (RestClientException e) {
            log.error("Error al comunicarse con Load Balancer: {}", e.getMessage());
            return Map.of(
                "status", "ERROR",
                "message", "Servicio de enrutamiento no disponible: " + e.getMessage()
            );
        }
    }

    // Fallback cuando el Load Balancer no responde, fijense en la notacion @cricuitBreaker
    public Map<String, Object> routeFallback(String operation,
            Map<String, Object> body,
            Throwable t) {
        log.error("Circuit Breaker [lb-calls] ABIERTO - Load Balancer no disponible");
        return Map.of(
                "status", "ERROR",
                "message", "Sistema temporalmente no disponible. Intente más tarde.");
    }
}
