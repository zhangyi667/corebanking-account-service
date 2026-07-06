package com.corebanking.account.api;

import com.corebanking.account.domain.Account;
import com.corebanking.account.service.AccountNotFoundException;
import com.corebanking.account.service.AccountService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;

@RestController
@RequestMapping("/api/v1")
public class AccountController {

    private final AccountService service;

    public AccountController(AccountService service) {
        this.service = service;
    }

    @PostMapping("/accounts")
    public ResponseEntity<AccountResponse> create(@Valid @RequestBody CreateAccountRequest req) {
        Account created = service.create(req.accountId(), req.ownerId(), req.currency());
        return ResponseEntity.created(URI.create("/api/v1/accounts/" + created.getId()))
                .body(AccountResponse.of(created));
    }

    @GetMapping("/accounts/{id}")
    public AccountResponse get(@PathVariable String id) {
        return service.get(id)
                .map(AccountResponse::of)
                .orElseThrow(() -> new AccountNotFoundException(id));
    }
}
