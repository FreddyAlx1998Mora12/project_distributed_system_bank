package unl.project.distributed.load_balancer_service.infrastructure.adapter.inbound.web;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;

@RestController
@RequestMapping("/circuit-breakers")
public class CircuitBreakerController {

    private final CircuitBreakerRegistry circuitBreakerRegistry;

    public CircuitBreakerController(CircuitBreakerRegistry circuitBreakerRegistry) {
        this.circuitBreakerRegistry = circuitBreakerRegistry;
    }

    @GetMapping("/status")
    public ResponseEntity<List<CircuitBreakerInfo>> getStatus() {
        List<CircuitBreakerInfo> breakers = circuitBreakerRegistry.getAllCircuitBreakers()
                .stream()
                .map(cb -> new CircuitBreakerInfo(
                        cb.getName(),
                        cb.getState().name(),
                        cb.getMetrics().getFailureRate()))
                .collect(Collectors.toList());
        return ResponseEntity.ok(breakers);
    }

    public record CircuitBreakerInfo(
            String name,
            String state,
            float failureRate) {
    }
}
