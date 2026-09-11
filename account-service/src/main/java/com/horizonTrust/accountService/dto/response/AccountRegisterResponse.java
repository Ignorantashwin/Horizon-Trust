package com.horizonTrust.accountService.dto.response;

import com.horizonTrust.accountService.enums.AccountStatus;
import com.horizonTrust.accountService.enums.AccountType;
import lombok.Builder;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Builder
public record AccountRegisterResponse(String id, String accountNo, String holderName, BigDecimal accountBalance,
                                      BigDecimal dailyLimit, AccountType accountType, AccountStatus status, String email,
                                      String phone, LocalDateTime createdAt, LocalDateTime updatedAt
) {
}
