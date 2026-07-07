package com.corebanking.account.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "outbox_event")
public class OutboxEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "event_type", nullable = false, length = 64)
    private String eventType;

    @Column(name = "aggregate_id", nullable = false)
    private UUID aggregateId;

    @Column(name = "partition_key", nullable = false, length = 128)
    private String partitionKey;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(nullable = false, columnDefinition = "jsonb")
    private String payload;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "sent_at")
    private Instant sentAt;

    @Column(nullable = false)
    private int attempts;

    protected OutboxEvent() {
    }

    public OutboxEvent(String eventType, UUID aggregateId, String partitionKey, String payload) {
        this.eventType = eventType;
        this.aggregateId = aggregateId;
        this.partitionKey = partitionKey;
        this.payload = payload;
        this.createdAt = Instant.now();
        this.attempts = 0;
    }

    public Long getId() { return id; }
    public String getEventType() { return eventType; }
    public UUID getAggregateId() { return aggregateId; }
    public String getPartitionKey() { return partitionKey; }
    public String getPayload() { return payload; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getSentAt() { return sentAt; }
    public int getAttempts() { return attempts; }

    public void markSent() {
        this.sentAt = Instant.now();
    }

    public void incrementAttempts() {
        this.attempts++;
    }
}
