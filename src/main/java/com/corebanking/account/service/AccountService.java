package com.corebanking.account.service;

import com.corebanking.account.domain.Account;
import com.corebanking.account.domain.AccountStatus;
import com.corebanking.account.repo.AccountRepository;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
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

    public List<Account> check(Collection<String> ids) {
        return repo.findAllByIdIn(ids);
    }

    public Account updateStatus(String accountId, AccountStatus newStatus) {
        Account a = repo.findById(accountId)
                .orElseThrow(() -> new AccountNotFoundException(accountId));
        a.setStatus(newStatus);
        return repo.save(a);
    }
}
