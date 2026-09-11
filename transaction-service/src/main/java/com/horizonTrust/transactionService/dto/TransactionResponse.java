package com.horizonTrust.transactionService.dto;

import com.horizonTrust.transactionService.enums.TransactionStatus;
import com.horizonTrust.transactionService.enums.TransactionType;
import lombok.Builder;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Builder
public record TransactionResponse(String id, String senderAccount, String receiverAccount,
                                  BigDecimal amount, TransactionType type, TransactionStatus status,
                                  String description, String referenceNumber, String failureReason,
                                  LocalDateTime createdAt, LocalDateTime completedAt) {
}
