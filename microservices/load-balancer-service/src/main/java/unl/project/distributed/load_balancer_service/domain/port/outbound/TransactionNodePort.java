package unl.project.distributed.load_balancer_service.domain.port.outbound;

import java.util.Map;

public interface TransactionNodePort {
    Map<String, Object> executeTransaction(String nodeId, String targetUrl, String operation, Map<String, Object> body);
}
