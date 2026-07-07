package com.corebanking.account.events;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Shape of the payload published by posting-manager on the
 * `posting.transaction` topic. Duplicated here (not shared with pm) so each
 * service can evolve its consumer independently — the contract lives in the
 * platform-level contracts/schemas repo, not in Java code.
 */
public record PostingTransactionReceived(
        UUID postingId,
        String transactionRef,
        String correlationId,
        String idempotencyKey,
        String currency,
        Instant receivedAt,
        List<Leg> legs,
        Map<String, Object> metadata
) {
    public record Leg(String accountId, LegType type, BigDecimal amount) {
    }

    public enum LegType {DEBIT, CREDIT}
}
