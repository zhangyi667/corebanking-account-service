package com.corebanking.account.service;

import com.corebanking.account.domain.Balance;
import com.corebanking.account.domain.OutboxEvent;
import com.corebanking.account.domain.ProcessedEvent;
import com.corebanking.account.events.PostingTransactionReceived;
import com.corebanking.account.repo.BalanceRepository;
import com.corebanking.account.repo.OutboxEventRepository;
import com.corebanking.account.repo.ProcessedEventRepository;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BalanceApplierTest {

    @Mock BalanceRepository balances;
    @Mock ProcessedEventRepository processed;
    @Mock OutboxEventRepository outbox;

    BalanceApplier applier;

    @BeforeEach
    void setUp() {
        applier = new BalanceApplier(balances, processed, outbox, JsonMapper.builder().build());
    }

    private static PostingTransactionReceived posting(String key) {
        return new PostingTransactionReceived(
                UUID.fromString("11111111-1111-1111-1111-111111111111"),
                "txn-1",
                "corr-1",
                key,
                "USD",
                Instant.parse("2026-07-07T10:00:00Z"),
                List.of(
                        new PostingTransactionReceived.Leg("acc-a", PostingTransactionReceived.LegType.DEBIT, new BigDecimal("100.00")),
                        new PostingTransactionReceived.Leg("acc-b", PostingTransactionReceived.LegType.CREDIT, new BigDecimal("100.00"))
                ),
                null
        );
    }

    @Test
    void applyDebitsAndCreditsBothLegsAndCachesTheDedupRow() {
        when(processed.existsById("k-1")).thenReturn(false);
        when(balances.findByAccountId("acc-a"))
                .thenReturn(Optional.of(new Balance("acc-a", new BigDecimal("500.00"), Instant.now())));
        when(balances.findByAccountId("acc-b"))
                .thenReturn(Optional.of(new Balance("acc-b", new BigDecimal("200.00"), Instant.now())));

        BalanceApplier.Result result = applier.apply(posting("k-1"));

        assertThat(result).isEqualTo(BalanceApplier.Result.APPLIED);

        ArgumentCaptor<Balance> saved = ArgumentCaptor.forClass(Balance.class);
        verify(balances, times(2)).save(saved.capture());
        Balance debit = saved.getAllValues().stream().filter(b -> b.getAccountId().equals("acc-a")).findFirst().orElseThrow();
        Balance credit = saved.getAllValues().stream().filter(b -> b.getAccountId().equals("acc-b")).findFirst().orElseThrow();
        assertThat(debit.getAmount()).isEqualByComparingTo("400.00");
        assertThat(credit.getAmount()).isEqualByComparingTo("300.00");

        verify(outbox, times(2)).save(any(OutboxEvent.class));
        verify(processed).save(any(ProcessedEvent.class));
    }

    @Test
    void duplicateIsDetectedAndNothingIsMutated() {
        when(processed.existsById("k-dup")).thenReturn(true);

        BalanceApplier.Result result = applier.apply(posting("k-dup"));

        assertThat(result).isEqualTo(BalanceApplier.Result.DUPLICATE);
        verify(balances, never()).save(any());
        verify(outbox, never()).save(any());
        verify(processed, never()).save(any());
    }

    @Test
    void missingBalanceRowInitializesFromZero() {
        when(processed.existsById("k-new")).thenReturn(false);
        when(balances.findByAccountId(any())).thenReturn(Optional.empty());

        applier.apply(posting("k-new"));

        ArgumentCaptor<Balance> saved = ArgumentCaptor.forClass(Balance.class);
        verify(balances, times(2)).save(saved.capture());
        // acc-a is DEBIT 100 → new balance -100
        // acc-b is CREDIT 100 → new balance 100
        Balance a = saved.getAllValues().stream().filter(b -> b.getAccountId().equals("acc-a")).findFirst().orElseThrow();
        Balance b = saved.getAllValues().stream().filter(x -> x.getAccountId().equals("acc-b")).findFirst().orElseThrow();
        assertThat(a.getAmount()).isEqualByComparingTo("-100.00");
        assertThat(b.getAmount()).isEqualByComparingTo("100.00");
    }
}
