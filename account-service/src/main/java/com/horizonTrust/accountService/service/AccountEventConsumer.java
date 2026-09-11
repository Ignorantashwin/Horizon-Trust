package com.horizonTrust.accountService.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.Map;
import java.util.Objects;

@Service
@Slf4j
@RequiredArgsConstructor
public class AccountEventConsumer {
private final AccountService accountService;
    /*
    consume transaction.completed
     */

    @KafkaListener(topics = "transaction.completed")
    public void consumeTransactionCompleted(@Payload Map<String, Object> payload){

        try{
           String receiverAccount = payload.get("receiverAccountNumber").toString();
            BigDecimal amount = new BigDecimal(payload.get("amount").toString());

            log.info("Crediting Account : {} amount{}", receiverAccount, amount);

            accountService.creditBalance(receiverAccount, amount);
        }catch (Exception e){
           log.error("CanNot complete payment due to error : {}", e.getMessage());
        }
    }

    /*
   consume fraud.detected event via Kafka
     */

    @KafkaListener(topics = "fraud.detected")
    public void consumeFraudDetected(@Payload Map<String, Object> payload){
        try{

            String accountNumber = payload.get("accountNumber").toString();
            log.info("Fraud detected : blocking account : {}", accountNumber);
            accountService.blockAccount(accountNumber);

        }catch (Exception e){
          log.error("cannot block account due to : {}", e.getMessage());
        }
    }

}
