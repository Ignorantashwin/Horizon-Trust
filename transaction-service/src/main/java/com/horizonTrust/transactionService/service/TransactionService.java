package com.horizonTrust.transactionService.service;

import com.horizonTrust.transactionService.client.AccountServiceClient;
import com.horizonTrust.transactionService.dto.TransactionResponse;
import com.horizonTrust.transactionService.dto.TransferRequest;
import com.horizonTrust.transactionService.entity.Transaction;
import com.horizonTrust.transactionService.enums.TransactionStatus;
import com.horizonTrust.transactionService.enums.TransactionType;
import com.horizonTrust.transactionService.event.TransactionInitiatedEvent;
import com.horizonTrust.transactionService.repository.TransactionRepo;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import java.time.LocalDateTime;
import java.util.List;
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


}
