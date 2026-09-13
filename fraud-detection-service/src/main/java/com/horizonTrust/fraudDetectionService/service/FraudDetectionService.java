package com.horizonTrust.fraudDetectionService.service;

import com.horizonTrust.fraudDetectionService.client.AccountServiceClient;
import com.horizonTrust.fraudDetectionService.model.FraudCheckResult;
import lombok.RequiredArgsConstructor;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
@Slf4j
public class FraudDetectionService {

    private final AccountServiceClient accountServiceClient;
    private static final String VERIFICATION_REQUIRED_TOPIC= "verification.required";
    private static final String CLEAN_TRANSACTION_TOPIC= "fraudCheck.clean";

    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final RedisTemplate<String, String> redisTemplate;

    @Value("${max-transaction-per-minute}")
    private int maxTransactionPerMinute;

    @Value("${max-fraud-transaction-multiplier}")
    private double maxFraudTransactionMultiplier;

    @Value("${max-balance-allowed-check}")
    private double maxBalancePercentage;


    public void checkTransaction(@Payload Map<String, Object> payload){
        log.info("Checking fraud for transaction : {}", payload.get("transactionId"));
        String transactionId = payload.get("transactionId").toString();
        String senderAccount = payload.get("senderAccount").toString();
        BigDecimal amount = new BigDecimal(payload.get("amount").toString());

        // fetch real account balance from service
   BigDecimal senderBalance = accountServiceClient.getBalance(senderAccount);
    log.info("Checking transaction : {} account : {} amount : {} balance : {} ", transactionId, senderAccount,amount, senderBalance);

        FraudCheckResult result = performFraudChecks(senderAccount, amount, senderBalance);

        if (result.isFraud()){
            log.info("Fraud detected with transaction - account : {} " + " reason : {} OTP required for verification", senderAccount, result.getReason());

            // now we found a fraud or suspicious activity so we need to make a event called verificationEvent

            Map<String, Object> verificationEvent = new HashMap<>();
            verificationEvent.put("transactionId", transactionId);
            verificationEvent.put("senderAccount", senderAccount);
            verificationEvent.put("amount", amount);
            verificationEvent.put("reason", result.getReason());

            kafkaTemplate.send(VERIFICATION_REQUIRED_TOPIC,transactionId, verificationEvent);

        }
        else {
            log.info("Transaction clean");

            Map<String, Object>cleanTransactionEvent = new HashMap<>();
            cleanTransactionEvent.put("transactionId", transactionId);
            cleanTransactionEvent.put("isFraud", false);
            cleanTransactionEvent.put("reason", null);
            kafkaTemplate.send(CLEAN_TRANSACTION_TOPIC, transactionId, cleanTransactionEvent);

        }
    }

    private FraudCheckResult performFraudChecks(String senderAccount, BigDecimal amount, BigDecimal senderBalance){
        // pattern 1

        if (isVelocityExceeded(senderAccount)){
            return new FraudCheckResult(true, "Too many Transactions is 60 seconds " + " - velocity limit exceeded");
        }

        if (isAmountSuspicious(senderAccount, amount)){
            return new FraudCheckResult(true, "Unusual transaction amount " + " - exceeds 3x your average");
        }

        if (senderBalance.compareTo(BigDecimal.ZERO) > 0 && isBalanceCheckFailed(senderBalance, amount)){
            return new FraudCheckResult(true, "Transactions exceeds 90% of balance");
        }
        return new FraudCheckResult(false, null);
    }

    private boolean isVelocityExceeded(String accountNumber){
        String key = "fraud:velocity" + accountNumber;
        Long count = redisTemplate.opsForValue().increment(key);
        if (count != null && count == 1){
            redisTemplate.expire(key, 60, TimeUnit.SECONDS);
        }
        return count != null && count > maxTransactionPerMinute;
    }

    private boolean isAmountSuspicious(String accountNumber, BigDecimal amount){
         String avgKey = "fraud:avg_amount" + accountNumber;
         String avgStr = redisTemplate.opsForValue().get(avgKey);

         if (avgStr == null){
             redisTemplate.opsForValue().set(avgKey, amount.toString());
             return false;
         }
         BigDecimal avgAmount= new BigDecimal(avgStr);
         BigDecimal threshold = avgAmount.multiply(BigDecimal.valueOf(maxFraudTransactionMultiplier));

         // update running average
        BigDecimal newAvg = avgAmount.add(amount).divide(BigDecimal.valueOf(2),2, RoundingMode.HALF_UP);
        redisTemplate.opsForValue().set(avgKey, newAvg.toString());
        return amount.compareTo(threshold) > 0;
    }

    private boolean isBalanceCheckFailed(BigDecimal balance , BigDecimal amount){
        BigDecimal maxAllowed = balance.multiply(BigDecimal.valueOf(maxBalancePercentage));
        log.info("Balance Check - amount : {} maxAllowed : {} suspicious : {}", amount, maxAllowed, amount.compareTo(maxAllowed) > 0);
        return amount.compareTo(maxAllowed) > 0;
    }
}
