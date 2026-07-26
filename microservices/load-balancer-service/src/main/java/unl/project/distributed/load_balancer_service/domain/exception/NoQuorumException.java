package unl.project.distributed.load_balancer_service.domain.exception;

public class NoQuorumException extends RuntimeException {
    public NoQuorumException(String message) {
        super(message);
    }
}
