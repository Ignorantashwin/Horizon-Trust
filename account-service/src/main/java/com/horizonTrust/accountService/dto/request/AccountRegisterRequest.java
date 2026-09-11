package com.horizonTrust.accountService.dto.request;

import com.horizonTrust.accountService.enums.AccountType;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Builder;

import java.math.BigDecimal;

@Builder
public record AccountRegisterRequest(@NotBlank(message = "Account holder name is required") String holderName,
                                     @NotBlank(message = "Phone is required") String phone,
                                     @NotBlank(message = "Email is required") @Email(message = "Invalid Email Format") String email,
                                     @NotNull(message = "Account type is required: CURRENT or SAVINGS") AccountType accountType,
                                     @NotNull(message = "Initial Deposit is required") @Positive(message = "Initial Deposit must be positive") BigDecimal initialDeposit) {
}
