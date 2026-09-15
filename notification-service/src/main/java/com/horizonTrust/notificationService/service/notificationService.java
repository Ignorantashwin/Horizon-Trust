package com.horizonTrust.notificationService.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.Map;

@Service
@Slf4j
public class notificationService {


    @KafkaListener(topics = "otp.generated")
    public void consumeOtpGenerated(@Payload Map<String, Object> payload){

        try{
            String accountNumber = (String) payload.get("senderAccount");
            String otp = (String) payload.get("otp");
            String amount = payload.get("amount").toString();
            String transactionId = payload.get("transactionId").toString();
            String reason = payload.get("reason").toString();

            sendAlert(accountNumber ,"TRANSACTION VERIFICATION REQUIRED",
                    String.format(
                            "Suspicious activity found in your account. " +
                            "Reason : %s " +
                            "A transaction of %s is pending verification. " +
                            "Your otp is %s. Valid for 5 minutes. " +
                            "If this wasn't you - Ignore this message. ",
                            reason, amount, otp
                    )
                    );

        } catch (Exception e) {
            log.error("Error Sending Otp notification : {}", e.getMessage());
        }
    }

    @KafkaListener(topics = "transaction.completed")
    public void consumeTransactionComplete(@Payload Map<String, Object> payload){

        try{

            String senderAccount = payload.get("senderAccount").toString();
            BigDecimal amount = new BigDecimal(payload.get("amount").toString());
            String receiverAccount = payload.get("receiverAccount").toString();

            // Debit Alert
            sendAlert(senderAccount,
                    "Debit Alert",
                    String.format(
                            "Transaction successful : " +
                                    " debited from account : %s " +
                                    "of amount : %s " +
                                    "to account : %s ",
                            senderAccount, amount, receiverAccount
                    )
                    );

            // credit alert

            sendAlert(receiverAccount,
                    "Credit Alert",
                    String.format(
                            "Received amount of %s " + 
                            "in account : %s " +
                            "from account : %s",
                            amount, receiverAccount, senderAccount
                    )
            );

        }catch (Exception e){
            log.error("Error sending transaction notification : {}", e.getMessage());
        }
    }

    // consume fraud detect event and alert user

    @KafkaListener(topics = "fraud.detected")
    public void consumeFraudDetected(@Payload Map<String, Object> payload){
        try{
            String accountNumber = payload.get("senderAccount").toString();
            String reason = payload.get("reason").toString();
            BigDecimal amount = new BigDecimal(payload.get("amount").toString());

            // fraud alert

            sendAlert(accountNumber,
                    "Suspicious activity detected :",
                    String.format(
                            "Your account %s has been blocked, "+
                            "reason : %s",
                            "please contact your bank immediately.",
                            accountNumber, reason
                    ));

        } catch (Exception e) {
           log.error("Error sending fraud detected notification");
        }
    }

    // consume refund transaction event and alert user

    @KafkaListener(topics = "transaction.refunded")
    public void consumeRefundTransaction(@Payload Map<String, Object> payload){

        try{

            String accountNumber = payload.get("senderAccount").toString();
            String reason = payload.get("reason").toString();
            BigDecimal amount = new BigDecimal(payload.get("amount").toString());

            // alert refund

            sendAlert(accountNumber,
                    "Refund alert :",
                    String.format(
                            "Your transaction of %s is failed, " +
                                    "reason : %s " +
                            "%s refunded to your account %s ",
                            amount, reason, amount, accountNumber
                    ));

        } catch (Exception e) {
         log.error("Error sending refund notification : {}", e.getMessage());
        }
    }

    // consume payment completed event and alert user

    @KafkaListener(topics = "payment.completed")
    public void consumePaymentCompleted(@Payload Map<String, Object> payload){
        try{
            String paymentId = payload.get("id").toString();
            String accountNumber = payload.get("senderAccount").toString();
            BigDecimal amount = new BigDecimal(payload.get("amount").toString());

            sendAlert(accountNumber,
                    "Payment successful : ",
                    String.format(
                            "Payment of %s is completed " +
                            "Razorpay id : %s ",
                            amount, payload.get("razorpayPaymentId")
                    )
                    );

        } catch (Exception e) {
           log.error("Error sending payment successful notification : {}", e.getMessage());
        }
    }

    public void consumeFailedPayment(@Payload Map<String,Object> payload){
          try{
              String accountNumber = payload.get("senderAccount").toString();
              BigDecimal amount = new BigDecimal(payload.get("amount").toString());
              String reason = payload.get("reason").toString();

              sendAlert(accountNumber,
                      "Payment failed : ",
                      String.format(
                              "Payment of %s is failed, " +
                              "reason : %s ",
                              amount, reason
                      )
                      );

          }catch (Exception e){
             log.error("Error sending payment failed notification : {}", e.getMessage());
          }
    }

    private void sendAlert(String accountNumber, String subject, String message){

    }
}
