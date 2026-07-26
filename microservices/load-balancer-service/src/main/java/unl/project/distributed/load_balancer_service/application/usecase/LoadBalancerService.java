package unl.project.distributed.load_balancer_service.application.usecase;

import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import unl.project.distributed.load_balancer_service.domain.exception.NoAvaliableNodeException;
import unl.project.distributed.load_balancer_service.domain.exception.NoQuorumException;
import unl.project.distributed.load_balancer_service.domain.model.ClusterTopology;
import unl.project.distributed.load_balancer_service.domain.model.HealthStatus;
import unl.project.distributed.load_balancer_service.domain.model.NodeStatus;
import unl.project.distributed.load_balancer_service.domain.port.inbound.RouteTransactionUseCase;
import unl.project.distributed.load_balancer_service.domain.port.outbound.ClusterMonitorPort;
import unl.project.distributed.load_balancer_service.domain.port.outbound.TransactionNodePort;

@Service
public class LoadBalancerService implements RouteTransactionUseCase {

    private static final Logger log = LoggerFactory.getLogger(LoadBalancerService.class);

    private final ClusterMonitorPort clusterMonitorPort;
    private final TransactionNodePort transactionNodePort;

    public LoadBalancerService(ClusterMonitorPort clusterMonitorPort, TransactionNodePort transactionNodePort) {
        this.clusterMonitorPort = clusterMonitorPort;
        this.transactionNodePort = transactionNodePort;
    }

    @Override
    public Map<String, Object> route(String operation, Map<String, Object> body) {
        log.info("LoadBalancer: evaluando topología del clúster para operación '{}'", operation);

        ClusterTopology topology = clusterMonitorPort.getTopology();
        if (topology == null || !topology.hasQuorum()) {
            log.error("LoadBalancer RECHAZADO: No hay quorum en el clúster. Prevención de Split-Brain activa.");
            throw new NoQuorumException("No hay quorum en el clúster. Las transacciones se encuentran bloqueadas.");
        }

        NodeStatus selectedNode = null;

        // 1. Priorizar el nodo Líder/Primario elegido por Quorum
        if (topology.leaderId() != null) {
            selectedNode = topology.nodes().stream()
                    .filter(n -> n.nodeId().equals(topology.leaderId()) && n.health() == HealthStatus.UP)
                    .findFirst()
                    .orElse(null);
        }

        // 2. Si no hay líder explícito o el líder no está sano, seleccionar cualquier nodo activo
        if (selectedNode == null) {
            selectedNode = topology.nodes().stream()
                    .filter(n -> n.health() == HealthStatus.UP)
                    .findFirst()
                    .orElse(null);
        }

        if (selectedNode == null) {
            log.error("LoadBalancer FALLO: No existe ningún nodo sano disponible en el clúster.");
            throw new NoAvaliableNodeException("No hay nodos de transacción disponibles para procesar la solicitud.");
        }

        log.info("LoadBalancer: enrutando transacción a nodo '{}' (URL: {})", selectedNode.nodeId(), selectedNode.url());
        return transactionNodePort.executeTransaction(selectedNode.nodeId(), selectedNode.url(), operation, body);
    }
}
