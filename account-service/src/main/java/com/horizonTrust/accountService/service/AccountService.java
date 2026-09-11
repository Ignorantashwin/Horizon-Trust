package com.horizonTrust.accountService.service;

import com.horizonTrust.accountService.dto.request.AccountRegisterRequest;
import com.horizonTrust.accountService.dto.response.AccountRegisterResponse;
import com.horizonTrust.accountService.dto.response.LoginAccountResponse;
import com.horizonTrust.accountService.entity.Account;
import com.horizonTrust.accountService.enums.AccountStatus;
import com.horizonTrust.accountService.enums.AccountType;
import com.horizonTrust.accountService.repository.AccountRepo;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class AccountService {
    private final AccountRepo accountRepo;
    private static SecureRandom secureRandom;

    public AccountRegisterResponse createAccount(AccountRegisterRequest request){
      log.info("Creating Account for : {}", request.email());

      if (accountRepo.existByEmail(request.email())){
          throw new RuntimeException("Account already Exist with email : " + request.email());
      }
      Account account = Account.builder()
              .accountHolderName(request.holderName())
              .email(request.email())
              .phone(request.phone())
              .accountNo(generateAccountNo())
              .accountBalance(request.initialDeposit())
              .accountType(AccountType.SAVINGS)
              .status(AccountStatus.ACTIVE)
              .accountDailyLimit(request.accountType() == AccountType.SAVINGS ? new BigDecimal("100000") : new BigDecimal("500000"))
              .createdAt(LocalDateTime.now())
              .build();
      Account savedAccount = accountRepo.save(account);

      log.info("Account registered with accountNumber : {}", account.getAccountNo());
      return AccountRegisterResponse.builder()
              .id(savedAccount.getId())
              .holderName(savedAccount.getAccountHolderName())
              .accountNo(savedAccount.getAccountNo())
              .accountBalance(savedAccount.getAccountBalance())
              .dailyLimit(savedAccount.getAccountDailyLimit())
              .email(savedAccount.getEmail())
              .phone(savedAccount.getPhone())
              .accountType(savedAccount.getAccountType())
              .status(savedAccount.getStatus())
              .createdAt(savedAccount.getCreatedAt())
              .build();

    }

    private String generateAccountNo(){
        String accountNumber;
        do {
            Long number = secureRandom.nextLong(1_000_000_000_000L);
            accountNumber = String.format("%012d", number);
        }while (accountRepo.existsAccountByAccountNo(accountNumber));
        return accountNumber;
    }


    public LoginAccountResponse getAccount(String accountNumber){
        Account account = accountRepo.findAccountByAccountNo(accountNumber).orElseThrow(()-> new RuntimeException("Account not exist : " + accountNumber));
        return LoginAccountResponse.builder()
                .id(account.getId())
                .accountNo(account.getAccountNo())
                .holderName(account.getAccountHolderName())
                .accountBalance(account.getAccountBalance())
                .dailyLimit(account.getAccountDailyLimit())
                .email(account.getEmail())
                .phone(account.getPhone())
                .accountType(account.getAccountType())
                .status(account.getStatus())
                .createdAt(account.getCreatedAt())
                .build();

    }

    public BigDecimal checkBalance(String accountNo){
        Account account = accountRepo.findAccountByAccountNo(accountNo).orElseThrow(()-> new RuntimeException("No account find with accountNumber : " + accountNo));
        return account.getAccountBalance();
    }

    /*
    called by fraud Detection service via Kafka
     */

    public void blockAccount(String accountNo){
        log.info("Blocking Account with accountNumber : {}", accountNo);
        Account account = accountRepo.findAccountByAccountNo(accountNo).orElseThrow(()-> new RuntimeException("No Account Find with accountNumber : " + accountNo));
        account.setStatus(AccountStatus.BLOCKED);
        accountRepo.save(account);
        log.info("Account Blocked Successfully with accountNumber : {}", accountNo);
    }


    /*
    implementing SAGA step 1
    deduct balance
     */

   public void deductBalance(String accountNo, BigDecimal amount){
       log.info("Deducting Balance from account : {}", accountNo);
    Account senderAccount = accountRepo.findAccountByAccountNo(accountNo).orElseThrow(()-> new RuntimeException("Sender Account doesn't exist"));
    if (senderAccount.getStatus() != AccountStatus.ACTIVE){
    throw new RuntimeException("Cannot process transaction because account status : " + senderAccount.getStatus());
  }
    if (senderAccount.getAccountBalance().compareTo(amount) < 0){
      throw new RuntimeException("payment declined! Insufficient Funds");
    }
     senderAccount.setAccountBalance(senderAccount.getAccountBalance().subtract(amount));
    accountRepo.save(senderAccount);
     log.info("Successfully deducted money from sender account : {}", accountNo);
     log.info("Balance Updated. New Balance : {}", senderAccount.getAccountBalance());

  }

  /*
  implementing SAGA step 4
  credit balance
   */

  public void creditBalance(String accountNo, BigDecimal amount){
       log.info("crediting Balance into account : {}", accountNo);
       Account receiverAccount = accountRepo.findAccountByAccountNo(accountNo).orElseThrow(()-> new RuntimeException("Account not exist : " + accountNo));
       if (receiverAccount.getStatus() != AccountStatus.ACTIVE){
           throw new RuntimeException("Payment failed! Receiver account status : " + receiverAccount.getStatus() );
       }
       receiverAccount.setAccountBalance(receiverAccount.getAccountBalance().add(amount));
       accountRepo.save(receiverAccount);
       log.info("Successfully credited into receiver account :{},  New Balance : {}", accountNo,receiverAccount.getAccountBalance());
  }
  }
