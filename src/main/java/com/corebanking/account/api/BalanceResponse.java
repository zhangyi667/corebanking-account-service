package com.corebanking.account.api;

import com.corebanking.account.domain.Balance;

import java.math.BigDecimal;
import java.time.Instant;

public record BalanceResponse(
        String accountId,
        BigDecimal amount,
        Instant updatedAt
) {
    public static BalanceResponse of(Balance b) {
        return new BalanceResponse(b.getAccountId(), b.getAmount(), b.getUpdatedAt());
    }
}
