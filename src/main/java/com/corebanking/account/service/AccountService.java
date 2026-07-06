package com.corebanking.account.service;

import com.corebanking.account.domain.Account;
import com.corebanking.account.domain.AccountStatus;
import com.corebanking.account.repo.AccountRepository;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Optional;

@Service
public class AccountService {

    private final AccountRepository repo;

    public AccountService(AccountRepository repo) {
        this.repo = repo;
    }

    public Account create(String accountId, String ownerId, String currency) {
        Account a = new Account(accountId, ownerId, currency, AccountStatus.ACTIVE, Instant.now());
        return repo.save(a);
    }

    public Optional<Account> get(String accountId) {
        return repo.findById(accountId);
    }
}
