package com.corebanking.account.api;

import com.corebanking.account.domain.Account;
import com.corebanking.account.repo.BalanceRepository;
import com.corebanking.account.service.AccountNotFoundException;
import com.corebanking.account.service.AccountService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1")
public class AccountController {

    private final AccountService service;
    private final BalanceRepository balances;

    public AccountController(AccountService service, BalanceRepository balances) {
        this.service = service;
        this.balances = balances;
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

    @PatchMapping("/accounts/{id}/status")
    public AccountResponse updateStatus(@PathVariable String id, @Valid @RequestBody UpdateStatusRequest req) {
        return AccountResponse.of(service.updateStatus(id, req.status()));
    }

    @GetMapping("/accounts/{id}/balance")
    public BalanceResponse balance(@PathVariable String id) {
        return balances.findById(id)
                .map(BalanceResponse::of)
                .orElseThrow(() -> new AccountNotFoundException(id));
    }

    @PostMapping("/accounts:check")
    public AccountCheckResponse check(@Valid @RequestBody AccountCheckRequest req) {
        List<Account> found = service.check(req.ids());
        Map<String, AccountResponse> byId = found.stream()
                .collect(Collectors.toMap(Account::getId, AccountResponse::of));
        Set<String> foundIds = new HashSet<>(byId.keySet());
        List<String> missing = req.ids().stream()
                .filter(id -> !foundIds.contains(id))
                .distinct()
                .toList();
        return new AccountCheckResponse(byId, missing);
    }
}
