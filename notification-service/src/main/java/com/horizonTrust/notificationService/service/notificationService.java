package com.horizonTrust.notificationService.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
@Slf4j
public class notificationService {

    @KafkaListener(topics = "otp.generated")
    public void consumeOtpGenerated(@Payload Map<String, Object> payload){

        try{
            String accountNumber = (String) payload.get("senderAccount");
            String Otp = (String) payload.get("otp");
            String amount = payload.get("amount").toString();
            String transactionId = payload.get("transactionId").toString();
            String reason = payload.get("reason").toString();

            sendAlert(accountNumber ,"TRANSACTION VERIFICATION REQUIRED",
                    String.format(
                            "Suspicious activity found in your account. " +
                            "Reason : %s " +
                            "A transaction of %s is pending verification. " +
                            "Your otp is %s. Valid for 5 minutes. " +
                            "If this wasn't you - Ignore this message. "
                    )
                    );

        } catch (Exception e) {
            log.error("Error Sending Otp notification : {}", e.getMessage());
        }
    }

    private void sendAlert(String accountNumber, String subject, String message){

    }
}
