package unl.project.distributed.quorum_monitor_service.domain.model;

/**
 * Configuración estática de un nodo del clúster (viene de application.yml).
 * 'priority' define el orden de preferencia en la elección de líder:
 * menor valor = mayor prioridad (node-1 con priority=1 gana sobre node-2 con
 * priority=2
 * cuando ambos están sanos). Esto es Bully simplificado y determinista.
 */
public record NodeConfig(String id, String url, int priority) {

}
