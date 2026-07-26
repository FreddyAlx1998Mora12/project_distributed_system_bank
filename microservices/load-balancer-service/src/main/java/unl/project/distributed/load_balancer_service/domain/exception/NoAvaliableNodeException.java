package unl.project.distributed.load_balancer_service.domain.exception;

public class NoAvaliableNodeException extends RuntimeException{

    public NoAvaliableNodeException(String mensaje){
        super(mensaje);
    }
}
