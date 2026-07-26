package unl.project.distributed.load_balancer_service.infrastructure.adapter.inbound.web;

import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import io.github.resilience4j.circuitbreaker.CallNotPermittedException;
import unl.project.distributed.load_balancer_service.domain.exception.NoAvaliableNodeException;
import unl.project.distributed.load_balancer_service.domain.exception.NoQuorumException;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(NoQuorumException.class)
    public ResponseEntity<Map<String, Object>> handleNoQuorum(NoQuorumException ex) {
        log.warn("Manejando NoQuorumException: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(Map.of(
                "status", "ERROR_NO_QUORUM",
                "message", ex.getMessage()
        ));
    }

    @ExceptionHandler(NoAvaliableNodeException.class)
    public ResponseEntity<Map<String, Object>> handleNoAvailableNode(NoAvaliableNodeException ex) {
        log.warn("Manejando NoAvaliableNodeException: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(Map.of(
                "status", "ERROR_NO_NODES",
                "message", ex.getMessage()
        ));
    }

    @ExceptionHandler(CallNotPermittedException.class)
    public ResponseEntity<Map<String, Object>> handleCircuitBreakerOpen(CallNotPermittedException ex) {
        log.warn("Circuit Breaker Abierto para el nodo: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(Map.of(
                "status", "CIRCUIT_BREAKER_OPEN",
                "message", "El nodo de transacción se encuentra temporalmente inhabilitado por fallos repetidos."
        ));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleGenericException(Exception ex) {
        log.error("Error no controlado en LoadBalancer: {}", ex.getMessage(), ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of(
                "status", "INTERNAL_ERROR",
                "message", ex.getMessage() != null ? ex.getMessage() : "Error interno en el Load Balancer."
        ));
    }
}
