package com.horizonTrust.transactionService.repository;

import com.horizonTrust.transactionService.entity.Transaction;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;


public interface TransactionRepo extends JpaRepository<Transaction, String> {
    Optional<Transaction> getTransactionById(String transactionId);

    List<Transaction> getTransactionBySenderAccountOrderByCreatedAtDesc(String senderAccount);
}
