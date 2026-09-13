package com.horizonTrust.fraudDetectionService.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.math.BigDecimal;

@FeignClient(name = "account-service", url = "${account.service.url}")
public interface AccountServiceClient {

    @GetMapping("/v1/accounts/balance/{accountNo}")
    BigDecimal getBalance(@PathVariable String accountNo);
}
