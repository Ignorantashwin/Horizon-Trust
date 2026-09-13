package com.horizonTrust.transactionService.service;

import com.horizonTrust.transactionService.client.AccountServiceClient;
import com.horizonTrust.transactionService.dto.TransactionResponse;
import com.horizonTrust.transactionService.dto.TransferRequest;
import com.horizonTrust.transactionService.entity.Transaction;
import com.horizonTrust.transactionService.enums.TransactionStatus;
import com.horizonTrust.transactionService.enums.TransactionType;
import com.horizonTrust.transactionService.event.TransactionCompletedEvent;
import com.horizonTrust.transactionService.event.TransactionInitiatedEvent;
import com.horizonTrust.transactionService.repository.TransactionRepo;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class TransactionService {
    private final TransactionRepo transactionRepo;
    private final AccountServiceClient accountServiceClient;

    private final KafkaTemplate<String, Object> kafkaTemplate;

    private static final String TRANSACTION_INITIATED_TOPIC = "transaction.initiated";
    private static final String TRANSACTION_COMPLETED_TOPIC = "transaction.completed";
    private static final String TRANSACTION_REFUNDED_TOPIC = "transaction.refunded";
    private static final String FRAUD_DETECTED_EVENT = "fraud.detected";

    private final RedisTemplate<String,String> redisTemplate;
    /**\
     * SAGA step 1 initiate transfer
     * deducts from sender via feign
     * saves transaction as PROCESSING
     * publish event to Kafka for fraud detection
     * returns
     *
     * @param request
     * @return
     */

    public TransactionResponse transferMoney(TransferRequest request){
   log.info("Transfer started SAGA - Transfer : {} -> {} amount : {}", request.senderAccount(),request.senderAccount(), request.amount());

 // step 1 deduct from sender account

   accountServiceClient.deductBalance(request.senderAccount(),request.amount());

 //step 2 save transaction with PROCESSING status

    Transaction transaction = Transaction.builder()
         .senderAccount(request.senderAccount())
         .receiverAccount(request.receiverAccount())
         .amount(request.amount())
         .description(request.description())
         .type(TransactionType.TRANSFER)
         .status(TransactionStatus.PROCESSING)
         .referenceNumber(UUID.randomUUID().toString())
            .createdAt(LocalDateTime.now())
         .build();

    Transaction savedTransaction = transactionRepo.save(transaction);

 // step 3 publish event to Kafka for fraud check SAGA step 2

        TransactionInitiatedEvent event = new TransactionInitiatedEvent(
                savedTransaction.getId(), savedTransaction.getSenderAccount(), savedTransaction.getReceiverAccount(),
                savedTransaction.getAmount(), savedTransaction.getDescription()
        );

        kafkaTemplate.send(TRANSACTION_INITIATED_TOPIC, savedTransaction.getId(), event);

    log.info("Transaction Initiated event published : {}", savedTransaction.getId());

    return TransactionResponse.builder()
        .id(savedTransaction.getId())
        .senderAccount(savedTransaction.getSenderAccount())
        .receiverAccount(savedTransaction.getReceiverAccount())
        .amount(savedTransaction.getAmount())
        .description(savedTransaction.getDescription())
        .type(savedTransaction.getType())
        .status(savedTransaction.getStatus())
        .referenceNumber(savedTransaction.getReferenceNumber())
        .failureReason(savedTransaction.getFailureReason())
        .createdAt(savedTransaction.getCreatedAt())
        .completedAt(savedTransaction.getCompletedAt())
        .build();

    }

    public TransactionResponse getTransaction(String transactionId){
        Transaction transaction = transactionRepo.getTransactionById(transactionId).orElseThrow(()-> new RuntimeException("No transaction found with id : " + transactionId));
        return TransactionResponse.builder()
                .id(transaction.getId())
                .senderAccount(transaction.getSenderAccount())
                .receiverAccount(transaction.getReceiverAccount())
                .amount(transaction.getAmount())
                .description(transaction.getDescription())
                .type(transaction.getType())
                .status(transaction.getStatus())
                .referenceNumber(transaction.getReferenceNumber())
                .failureReason(transaction.getFailureReason())
                .createdAt(transaction.getCreatedAt())
                .completedAt(transaction.getCompletedAt())
                .build();
    }

    public List<TransactionResponse> getTransactionHistory(String accountNumber){
       return transactionRepo.getTransactionBySenderAccountOrderByCreatedAtDesc(accountNumber).stream().map(transaction -> TransactionResponse.builder()
               .id(transaction.getId())
               .senderAccount(transaction.getSenderAccount())
               .receiverAccount(transaction.getReceiverAccount())
               .amount(transaction.getAmount())
               .description(transaction.getDescription())
               .type(transaction.getType())
               .status(transaction.getStatus())
               .referenceNumber(transaction.getReferenceNumber())
               .failureReason(transaction.getFailureReason())
               .createdAt(transaction.getCreatedAt())
               .completedAt(transaction.getCompletedAt())
               .build())
               .toList();
    }


    public TransactionResponse verifyOTP(String transactionId, String otp) {
        Transaction transaction = transactionRepo.findById(transactionId).orElseThrow(()-> new RuntimeException("Transaction Not Found"));
        String otpKey = "verification:otp"+ transactionId;
        String storedOtp = redisTemplate.opsForValue().get(otpKey);
        if (storedOtp == null){
            log.warn("Otp expired for transaction : {}", transaction);
            compensateTransaction(transaction, "Otp expired -  transaction cancelled and amount refunded ");
            return TransactionResponse.builder()
                    .id(transaction.getId())
                    .senderAccount(transaction.getSenderAccount())
                    .receiverAccount(transaction.getReceiverAccount())
                    .amount(transaction.getAmount())
                    .description(transaction.getDescription())
                    .type(transaction.getType())
                    .status(transaction.getStatus())
                    .referenceNumber(transaction.getReferenceNumber())
                    .failureReason(transaction.getFailureReason())
                    .createdAt(transaction.getCreatedAt())
                    .completedAt(transaction.getCompletedAt())
                    .build();
        }

        if (!storedOtp.equals(otp)){
            // wrong otp cancel transaction and block account
            log.warn("Wrong Otp entered - blocking account and refunding amount : {}", transactionId);
            redisTemplate.delete(otpKey);
            blockAccountAndCompensate(transaction, "Wrong otp entered - Account blocked for security reasons : " + transaction.getSenderAccount());
            return TransactionResponse.builder()
                    .id(transaction.getId())
                    .senderAccount(transaction.getSenderAccount())
                    .receiverAccount(transaction.getReceiverAccount())
                    .amount(transaction.getAmount())
                    .description(transaction.getDescription())
                    .type(transaction.getType())
                    .status(transaction.getStatus())
                    .referenceNumber(transaction.getReferenceNumber())
                    .failureReason(transaction.getFailureReason())
                    .createdAt(transaction.getCreatedAt())
                    .completedAt(transaction.getCompletedAt())
                    .build();
        }

        // OTP verified - complete transaction

        log.info("OTP verified - completing transaction : {}", transactionId);
        redisTemplate.delete(otpKey);
        completeTransaction(transaction);
        return TransactionResponse.builder()
                .id(transaction.getId())
                .senderAccount(transaction.getSenderAccount())
                .receiverAccount(transaction.getReceiverAccount())
                .amount(transaction.getAmount())
                .description(transaction.getDescription())
                .type(transaction.getType())
                .status(transaction.getStatus())
                .referenceNumber(transaction.getReferenceNumber())
                .failureReason(transaction.getFailureReason())
                .createdAt(transaction.getCreatedAt())
                .completedAt(transaction.getCompletedAt())
                .build();
    }

    private void compensateTransaction(Transaction transaction , String reason){
        accountServiceClient.creditBalance(transaction.getSenderAccount(), transaction.getAmount());
         transaction.setStatus(TransactionStatus.REFUNDED);
         transactionRepo.save(transaction);
        Map<String, Object> refundEvent= new HashMap<>();
        refundEvent.put("transactionId", transaction.getId());
        refundEvent.put("senderAccount", transaction.getSenderAccount());
        refundEvent.put("amount", transaction.getAmount());
        refundEvent.put( "reason" ,reason);

         kafkaTemplate.send(TRANSACTION_REFUNDED_TOPIC, transaction.getId(),refundEvent );
    }

    private void blockAccountAndCompensate(Transaction transaction , String reason){
      // publish event fraud.detected account service consume and block the account

        Map<String, Object> fraudEvent = new HashMap<>();
        fraudEvent.put("transactionId", transaction.getId());
        fraudEvent.put("accountNumber", transaction.getSenderAccount());
        fraudEvent.put("amount", transaction.getAmount());
        fraudEvent.put("reason", reason);

        kafkaTemplate.send(FRAUD_DETECTED_EVENT, transaction.getSenderAccount(), fraudEvent);
        log.warn("Fraud.detected published - Account : {} will be blocked, kindly contact to your bank ", transaction.getSenderAccount() );

        // SAGA compensation - refund amount
        compensateTransaction(transaction, reason);

    }

    private void completeTransaction(Transaction transaction){
        log.info("No fraud detected, completing transaction : {}", transaction.getId());
        transaction.setStatus(TransactionStatus.COMPLETED);
        transaction.setCompletedAt(LocalDateTime.now());
        transactionRepo.save(transaction);

        TransactionCompletedEvent completedEvent = new TransactionCompletedEvent(
                transaction.getId(),
                transaction.getSenderAccount(),
                transaction.getReceiverAccount(),
                transaction.getAmount(),
                transaction.getDescription()
        );
        kafkaTemplate.send(TRANSACTION_COMPLETED_TOPIC, transaction.getId(), completedEvent);

    }

    public void processCleanTransaction(String transactionId) {
       Transaction transaction = transactionRepo.findById(transactionId)
               .orElseThrow(()-> new RuntimeException("Transaction not found"));

       if (transaction.getStatus() != TransactionStatus.PROCESSING){
           log.warn("transaction Not Processing - skipping : {}", transactionId);
       }
        completeTransaction(transaction);

    }
}
