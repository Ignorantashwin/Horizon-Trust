package com.horizonTrust.paymentService.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;

public record CreatePaymentRequest(@NotBlank(message = "account number must be required") String accountNumber,
                                   @NotNull(message = "amount must be required") @Positive(message = "amount must be positive") BigDecimal amount,
                                   String description) {
}
