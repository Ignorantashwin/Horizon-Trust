package com.horizonTrust.fraudDetectionService.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class FraudDetectionEventConsumer {
    private final FraudDetectionService fraudDetectionService;

    /**
     * listens to transaction. initiated topic
     * every transaction goes through fraud check before completion
     * @param payload
     */

    @KafkaListener(topics = "transaction.initiated", groupId = "fraud-detection-group ")
    public void consumeTransactionInitiated(@Payload Map<String, Object> payload){
        log.info("received Transaction for fraud detection : {}", payload.get("transactionId"));
        try{
            fraudDetectionService.checkTransaction(payload);

        } catch (Exception e) {
            throw new RuntimeException(e.getMessage());
        }
    }
}
