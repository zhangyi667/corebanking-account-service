package com.corebanking.account.api;

import com.corebanking.account.domain.Account;
import com.corebanking.account.domain.AccountStatus;

import java.time.Instant;

public record AccountResponse(
        String accountId,
        String ownerId,
        String currency,
        AccountStatus status,
        Instant createdAt
) {
    public static AccountResponse of(Account a) {
        return new AccountResponse(a.getId(), a.getOwnerId(), a.getCurrency(), a.getStatus(), a.getCreatedAt());
    }
}
