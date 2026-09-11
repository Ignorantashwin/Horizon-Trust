package com.horizonTrust.accountService.controller;

import com.horizonTrust.accountService.dto.request.AccountRegisterRequest;
import com.horizonTrust.accountService.dto.response.AccountRegisterResponse;
import com.horizonTrust.accountService.dto.response.LoginAccountResponse;
import com.horizonTrust.accountService.service.AccountService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;

@RestController
@RequestMapping("/v1/accounts")
@RequiredArgsConstructor
public class AccountController {
    private final AccountService accountService;

    @PostMapping("/register")
    public ResponseEntity<AccountRegisterResponse> registerAccount(@Valid @RequestBody AccountRegisterRequest request){
        AccountRegisterResponse response = accountService.createAccount(request);
        return ResponseEntity.ok().body(response);
    }

    @GetMapping("/get/{accountNo}")
    public ResponseEntity<LoginAccountResponse> getAccount(@PathVariable String accountNo){
        return ResponseEntity.ok(accountService.getAccount(accountNo));
    }

    @GetMapping("/balance/{accountNo}")
    public ResponseEntity<BigDecimal> getBalance(@PathVariable String accountNo){
        return ResponseEntity.ok(accountService.checkBalance(accountNo));
    }

    @PutMapping("/block/{accountNo}")
    public ResponseEntity<String> blockAccount(@PathVariable String accountNo){
        accountService.blockAccount(accountNo);
        return ResponseEntity.ok().body("Account Blocked Successfully");
    }

    /*
    SAGA pattern step 1 - deduct balance
    called by TransactionService when transaction or payment is initiated
     */

    @PutMapping("/deduct/{accountNo}")
    public ResponseEntity<String>deductBalance(@PathVariable String accountNo, @RequestParam BigDecimal amount){
        accountService.deductBalance(accountNo, amount);
        return ResponseEntity.ok().body(amount + " deducted from sender account : " + accountNo);
    }

    /*
    SAGA step 4 - compensating transaction endpoint
    called by Transaction Service in 2 cases
    1. fraud detected - Refund Sender - undo step 1
    2. payment Completed - credit receiver
     */

    @PutMapping("/credit/{accountNo}")
    public ResponseEntity<String>creditBalance(@PathVariable String accountNo, @RequestParam BigDecimal amount){
        accountService.creditBalance(accountNo, amount);
        return ResponseEntity.ok().body(amount + "successfully credited to account :" + accountNo);
    }


}
