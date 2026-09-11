package com.horizonTrust.accountService.repository;

import com.horizonTrust.accountService.entity.Account;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface AccountRepo extends JpaRepository<Account, String> {
    boolean existByEmail(String email);
    boolean existsAccountByAccountNo(String accountNo);
   Optional<Account> findAccountByAccountNo(String accountNo);

}
