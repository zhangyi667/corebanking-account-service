package com.corebanking.account.service;

import com.corebanking.account.domain.Balance;
import com.corebanking.account.domain.OutboxEvent;
import com.corebanking.account.domain.ProcessedEvent;
import com.corebanking.account.events.PostingTransactionReceived;
import com.corebanking.account.repo.BalanceRepository;
import com.corebanking.account.repo.OutboxEventRepository;
import com.corebanking.account.repo.ProcessedEventRepository;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Applies a posting to two account balances atomically. Idempotent by
 * {@code posting.idempotencyKey}: repeated deliveries of the same event skip
 * without side effects. Writes one outbox row per leg for downstream
 * {@code account.balance.changed} emission.
 */
@Service
public class BalanceApplier {

    private static final Logger log = LoggerFactory.getLogger(BalanceApplier.class);

    private final BalanceRepository balances;
    private final ProcessedEventRepository processed;
    private final OutboxEventRepository outbox;
    private final ObjectMapper mapper;

    public BalanceApplier(BalanceRepository balances,
                          ProcessedEventRepository processed,
                          OutboxEventRepository outbox,
                          ObjectMapper mapper) {
        this.balances = balances;
        this.processed = processed;
        this.outbox = outbox;
        this.mapper = mapper;
    }

    @Transactional
    public Result apply(PostingTransactionReceived posting) {
        if (processed.existsById(posting.idempotencyKey())) {
            return Result.DUPLICATE;
        }

        // Order legs by accountId so any two concurrent postings touching the
        // same pair always acquire row locks in the same sequence — deadlock-free.
        PostingTransactionReceived.Leg[] ordered = posting.legs().toArray(new PostingTransactionReceived.Leg[0]);
        if (ordered.length != 2) {
            throw new IllegalArgumentException("expected exactly 2 legs, got " + ordered.length);
        }
        if (ordered[0].accountId().compareTo(ordered[1].accountId()) > 0) {
            PostingTransactionReceived.Leg tmp = ordered[0];
            ordered[0] = ordered[1];
            ordered[1] = tmp;
        }

        Instant appliedAt = Instant.now();
        for (PostingTransactionReceived.Leg leg : ordered) {
            BigDecimal delta = leg.type() == PostingTransactionReceived.LegType.DEBIT
                    ? leg.amount().negate()
                    : leg.amount();
            Balance balance = balances.findByAccountId(leg.accountId())
                    .orElseGet(() -> new Balance(leg.accountId(), BigDecimal.ZERO, appliedAt));
            balance.apply(delta, appliedAt);
            balances.save(balance);

            outbox.save(new OutboxEvent(
                    "account.balance.changed",
                    posting.postingId(),
                    posting.correlationId(),
                    serializeBalanceChanged(posting, leg, delta, balance.getAmount(), appliedAt)
            ));
        }

        try {
            processed.save(new ProcessedEvent(posting.idempotencyKey(), posting.postingId(), appliedAt));
        } catch (DataIntegrityViolationException e) {
            // Rare: another Kafka delivery of the same event committed the dedup row between
            // our earlier check and this insert. Rollback the whole transaction so no double-apply.
            log.warn("concurrent duplicate for idempotencyKey={}, rolling back", posting.idempotencyKey());
            throw e;
        }
        return Result.APPLIED;
    }

    private String serializeBalanceChanged(PostingTransactionReceived posting,
                                           PostingTransactionReceived.Leg leg,
                                           BigDecimal delta,
                                           BigDecimal newBalance,
                                           Instant appliedAt) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("postingId", posting.postingId().toString());
        payload.put("correlationId", posting.correlationId());
        payload.put("accountId", leg.accountId());
        payload.put("currency", posting.currency());
        payload.put("delta", delta.toPlainString());
        payload.put("newBalance", newBalance.toPlainString());
        payload.put("appliedAt", appliedAt.toString());
        try {
            return mapper.writeValueAsString(payload);
        } catch (JacksonException e) {
            throw new IllegalStateException("failed to serialize balance-changed payload", e);
        }
    }

    public enum Result {
        APPLIED,
        DUPLICATE
    }
}
