package unl.project.distributed.apigateway.controller;

import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import unl.project.distributed.apigateway.service.RoutingService;

/**
 * Único punto de entrada externo del sistema (Máquina 1).
 * No contiene lógica de negocio bancaria: solo recibe y enruta.
 */
@RestController
@RequestMapping("/api/transactions")
public class TransactionController {

    private final RoutingService routingService;

    public TransactionController(RoutingService routingService) {
        this.routingService = routingService;
    }

    @PostMapping("/deposit")
    public ResponseEntity<Map<String, Object>> deposit(@RequestBody Map<String, Object> body) {
        return ResponseEntity.ok(routingService.route("deposit", body));
    }

    @PostMapping("/withdraw")
    public ResponseEntity<Map<String, Object>> withdraw(@RequestBody Map<String, Object> body) {
        return ResponseEntity.ok(routingService.route("withdraw", body));
    }

    @PostMapping("/transfer")
    public ResponseEntity<Map<String, Object>> transfer(@RequestBody Map<String, Object> body) {
        return ResponseEntity.ok(routingService.route("transfer", body));
    }
}
