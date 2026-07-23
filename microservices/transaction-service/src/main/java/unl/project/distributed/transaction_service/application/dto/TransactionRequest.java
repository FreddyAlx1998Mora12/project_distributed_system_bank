package unl.project.distributed.transaction_service.application.dto;

import java.math.BigDecimal;

public record TransactionRequest(String fromAccountId, String toAccountId, BigDecimal amount) {}
