package com.corebanking.account.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "processed_event")
public class ProcessedEvent {

    @Id
    @Column(name = "idempotency_key", length = 128)
    private String idempotencyKey;

    @Column(name = "posting_id", nullable = false)
    private UUID postingId;

    @Column(name = "processed_at", nullable = false)
    private Instant processedAt;

    protected ProcessedEvent() {
    }

    public ProcessedEvent(String idempotencyKey, UUID postingId, Instant processedAt) {
        this.idempotencyKey = idempotencyKey;
        this.postingId = postingId;
        this.processedAt = processedAt;
    }

    public String getIdempotencyKey() { return idempotencyKey; }
    public UUID getPostingId() { return postingId; }
    public Instant getProcessedAt() { return processedAt; }
}
