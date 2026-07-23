package unl.project.distributed.transaction_service.adapter.inbound.web;



import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import unl.project.distributed.transaction_service.application.dto.TransactionRequest;
import unl.project.distributed.transaction_service.application.dto.TransactionResponse;
import unl.project.distributed.transaction_service.application.usecase.ProcessTransactionUseCase;

@RestController
@RequestMapping("/transactions")
public class TransactionController {

    private final ProcessTransactionUseCase useCase;

    public TransactionController(ProcessTransactionUseCase useCase) {
        this.useCase = useCase;
    }

    @PostMapping("/deposit")
    public ResponseEntity<TransactionResponse> deposit(@RequestBody TransactionRequest req) {
        return respond(useCase.deposit(req));
    }

    @PostMapping("/withdraw")
    public ResponseEntity<TransactionResponse> withdraw(@RequestBody TransactionRequest req) {
        return respond(useCase.withdraw(req));
    }

    @PostMapping("/transfer")
    public ResponseEntity<TransactionResponse> transfer(@RequestBody TransactionRequest req) {
        return respond(useCase.transfer(req));
    }

    private ResponseEntity<TransactionResponse> respond(TransactionResponse resp) {
        return "COMMITTED".equals(resp.status()) ? ResponseEntity.ok(resp) : ResponseEntity.unprocessableEntity().body(resp);
    }
}
