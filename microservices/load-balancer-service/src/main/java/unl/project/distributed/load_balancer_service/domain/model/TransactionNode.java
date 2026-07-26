package unl.project.distributed.load_balancer_service.domain.model;


public record TransactionNode(
        String id,
        String url,
        NodeRole role,
        boolean active) {
    public TransactionNode withRole(NodeRole newRole) {
        return new TransactionNode(this.id, this.url, newRole, this.active);
    }

    public TransactionNode withActive(boolean newActive) {
        return new TransactionNode(this.id, this.url, this.role, newActive);
    }
}