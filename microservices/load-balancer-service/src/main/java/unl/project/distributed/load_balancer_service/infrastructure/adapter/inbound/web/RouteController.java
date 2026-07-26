package unl.project.distributed.load_balancer_service.infrastructure.adapter.inbound.web;

import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import unl.project.distributed.load_balancer_service.domain.port.inbound.RouteTransactionUseCase;

@RestController
@RequestMapping("/route")
public class RouteController {

    private static final Logger log = LoggerFactory.getLogger(RouteController.class);

    private final RouteTransactionUseCase routeTransactionUseCase;

    public RouteController(RouteTransactionUseCase routeTransactionUseCase) {
        this.routeTransactionUseCase = routeTransactionUseCase;
    }

    @PostMapping("/{operation}")
    public ResponseEntity<Map<String, Object>> route(
            @PathVariable String operation,
            @RequestBody Map<String, Object> body) {
        log.info("LoadBalancer Endpoint recibido: POST /route/{}", operation);
        Map<String, Object> response = routeTransactionUseCase.route(operation, body);
        return ResponseEntity.ok(response);
    }
}
