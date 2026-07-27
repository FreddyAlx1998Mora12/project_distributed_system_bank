package unl.project.distributed.transaction_service.application.dto;

public record TransactionResponse(String transactionId, String status, String message, String node) {}
