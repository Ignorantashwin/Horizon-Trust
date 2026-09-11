package com.horizonTrust.accountService.entity;

import com.horizonTrust.accountService.enums.AccountStatus;
import com.horizonTrust.accountService.enums.AccountType;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name ="accounts")
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class Account {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @Column(nullable = false, unique = true)
    private String accountNo;

    @Column(nullable = false)
    private String accountHolderName;

    @Column(nullable = false)
     private BigDecimal accountBalance;

     private BigDecimal accountDailyLimit;

     @Column(nullable = false, unique = true)
     private String email;

     @Column(nullable = false, unique = true, length = 10)
     private String phone;

     @Enumerated(EnumType.STRING)
     @Column(nullable = false)
     private AccountType accountType;

     @Enumerated(EnumType.STRING)
     @Column(nullable = false)
     private AccountStatus status;

     @CreationTimestamp
    private LocalDateTime createdAt;

     @UpdateTimestamp
    private LocalDateTime updatedAt;
}
