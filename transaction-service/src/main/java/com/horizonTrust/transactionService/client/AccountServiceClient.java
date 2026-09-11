package com.horizonTrust.transactionService.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.math.BigDecimal;

@FeignClient(name = "account-service", url = "${account.service.url}")
public interface AccountServiceClient {

    @PutMapping("/v1/accounts//deduct/{accountNo}")
    String deductBalance(@PathVariable String accountNo, @RequestParam BigDecimal amount);
}
