package unl.project.distributed.load_balancer_service.domain.port.inbound;

import java.util.Map;

public interface RouteTransactionUseCase {
    Map<String, Object> route(String operation, Map<String, Object> body);
}
