package com.corebanking.account.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "balance")
public class Balance {

    @Id
    @Column(name = "account_id", length = 64)
    private String accountId;

    @Column(nullable = false, precision = 19, scale = 4)
    private BigDecimal amount;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Version
    private long version;

    protected Balance() {
    }

    public Balance(String accountId, BigDecimal amount, Instant updatedAt) {
        this.accountId = accountId;
        this.amount = amount;
        this.updatedAt = updatedAt;
    }

    public String getAccountId() { return accountId; }
    public BigDecimal getAmount() { return amount; }
    public Instant getUpdatedAt() { return updatedAt; }
    public long getVersion() { return version; }

    public void apply(BigDecimal delta, Instant appliedAt) {
        this.amount = this.amount.add(delta);
        this.updatedAt = appliedAt;
    }
}
