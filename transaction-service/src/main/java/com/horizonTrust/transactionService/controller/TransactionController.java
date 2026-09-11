package com.horizonTrust.transactionService.controller;

import com.horizonTrust.transactionService.dto.TransactionResponse;
import com.horizonTrust.transactionService.dto.TransferRequest;
import com.horizonTrust.transactionService.service.TransactionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;


@RestController
@RequestMapping("/transaction/v1")
@RequiredArgsConstructor
@Slf4j

public class TransactionController {
    private final TransactionService transactionService;

    @PostMapping("transfer")
    public ResponseEntity<TransactionResponse> transfer(@Valid @RequestBody TransferRequest request){
        TransactionResponse response = transactionService.transferMoney(request);
        return ResponseEntity.ok().body(response);
    }

    @GetMapping("/{transactionId}")
    public ResponseEntity<TransactionResponse> getTransaction(@PathVariable String transactionId){
        TransactionResponse response = transactionService.getTransaction(transactionId);
        return ResponseEntity.ok().body(response);
    }

    @GetMapping("/account/{accountNumber}")
    public ResponseEntity<List<TransactionResponse>> getTransactionHistory(@PathVariable String accountNumber){
        return ResponseEntity.ok(transactionService.getTransactionHistory(accountNumber));
    }

    // otp verification endPoints

    @PostMapping("/verify/{transactionId}")
    public ResponseEntity<TransactionResponse> verifyOTP(@PathVariable String transactionId, @RequestParam String otp){
        log.info("Verify OTP request - transaction id : {}", transactionId);
        return ResponseEntity.ok(transactionService.verifyOTP(transactionId,otp));
    }



//
//    public ResponseEntity<TransactionResponse> withdrawal(@PathVariable String accountNumber, BigDecimal amount){
//
//    }

}
