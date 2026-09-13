package com.horizonTrust.transactionService.service;

import com.horizonTrust.transactionService.entity.Transaction;
import com.horizonTrust.transactionService.enums.TransactionStatus;
import com.horizonTrust.transactionService.repository.TransactionRepo;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Service
@Slf4j
@RequiredArgsConstructor
public class TransactionEventConsumer {
    private final TransactionRepo transactionRepo;
    private final RedisTemplate<String, String> redisTemplate;
    private final KafkaTemplate<String, Object> kafkaTemplate;

    private final TransactionService transactionService;

    private static final String TRANSACTION_OTP_GENERATED_TOPIC = "otp.generated";

    private static final long OTP_EXPIRY_MINUTES= 5;

    @KafkaListener(topics = "verification.required")

    public void consumeVerificationRequired(@Payload Map<String, Object> payload){

        try {

            String transactionId = payload.get("transactionId").toString();
            String senderAccount = payload.get("senderAccount").toString();
            String reason = payload.get("reason").toString();

            log.info("Verification Required : transaction : {} reason : {}", transactionId, reason);

            Transaction transaction = transactionRepo.findById(transactionId).orElseThrow(()-> new RuntimeException("Transaction Not Found " + transactionId));

            if (transaction.getStatus() != TransactionStatus.PROCESSING){
                log.warn("Transaction : {} is not Processing ", transactionId);
                return;
            }
            // generate 6 digit OTP
            String otp = String.format("%06d", (int)(Math.random() * 900000 ) + 100000);
            // Store otp in redis and expires in 5 minutes
         String otpKey = "verification:otp"+ transactionId;
         redisTemplate.opsForValue().set(otpKey, otp, OTP_EXPIRY_MINUTES, TimeUnit.MINUTES);

         // update status

            transaction.setStatus(TransactionStatus.PENDING_VERIFICATION);
            transactionRepo.save(transaction);

            log.info("Otp generated for transaction : {} expires in minutes : {}", transactionId, OTP_EXPIRY_MINUTES);

            // Notify user make an event consumed by notification service

            Map<String, Object> otpEvent = new HashMap<>();
            otpEvent.put("transactionId", transactionId);
            otpEvent.put("otp", otp);
            otpEvent.put("reason", reason);
            otpEvent.put("accountNumber", senderAccount);
            otpEvent.put("amount", payload.get("amount"));

            kafkaTemplate.send(TRANSACTION_OTP_GENERATED_TOPIC, transactionId, otpEvent);

        }catch (Exception e){
            log.error("Error Handling Verification Required : {}", e.getMessage());

        }
    }

    @KafkaListener(topics = "fraudCheck.clean")
    public void consumeFraudCheckCleanEvent(@Payload Map<String, Object> payload){

        try{
            String transactionId = payload.get("transactionId").toString();
            transactionService.processCleanTransaction(transactionId);
            log.info("Completing clean transaction - {}", transactionId);



        }catch (Exception e){
          log.error("Error Completing Transaction Failed : {}",  e.getMessage());
        }

    }

}
