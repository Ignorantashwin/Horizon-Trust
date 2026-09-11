package com.horizonTrust.transactionService.dto;

import com.horizonTrust.transactionService.enums.TransactionStatus;
import com.horizonTrust.transactionService.enums.TransactionType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record TransferRequest(@NotBlank String receiverAccount, @NotBlank String senderAccount, @NotNull BigDecimal amount, @NotNull
                              TransactionStatus status, @NotNull TransactionType type, String description) {
}
