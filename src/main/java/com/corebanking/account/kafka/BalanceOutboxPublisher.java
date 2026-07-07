package com.corebanking.account.kafka;

import com.corebanking.account.domain.OutboxEvent;
import com.corebanking.account.repo.OutboxEventRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Drains the outbox table to Kafka. Runs on a scheduled interval; each poll
 * pulls a bounded batch under SKIP LOCKED, publishes each row synchronously,
 * marks it sent on success. Failure at Kafka send leaves the row unsent — the
 * next poll retries.
 */
@Component
@EnableScheduling
public class BalanceOutboxPublisher {

    private static final Logger log = LoggerFactory.getLogger(BalanceOutboxPublisher.class);
    private static final int BATCH = 100;
    private static final long SEND_TIMEOUT_MS = 5_000;

    private final OutboxEventRepository repo;
    private final KafkaTemplate<byte[], byte[]> template;
    private final String topic;
    private final AtomicLong sequence = new AtomicLong();

    public BalanceOutboxPublisher(OutboxEventRepository repo,
                                  KafkaTemplate<byte[], byte[]> template,
                                  @Value("${app.kafka.topics.account-balance}") String topic) {
        this.repo = repo;
        this.template = template;
        this.topic = topic;
    }

    @Scheduled(fixedDelayString = "${app.outbox.poll-interval-ms:200}")
    @Transactional
    public void drain() {
        List<OutboxEvent> batch = repo.pickUnsent(PageRequest.of(0, BATCH));
        for (OutboxEvent row : batch) {
            String envelope = buildEnvelope(row);
            try {
                template.send(topic, row.getPartitionKey().getBytes(), envelope.getBytes())
                        .get(SEND_TIMEOUT_MS, TimeUnit.MILLISECONDS);
                row.markSent();
                repo.save(row);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                log.warn("outbox publish interrupted for row {}", row.getId());
                return;
            } catch (ExecutionException | TimeoutException e) {
                row.incrementAttempts();
                repo.save(row);
                log.warn("outbox publish failed for row {} (attempt {})", row.getId(), row.getAttempts(), e);
                return;
            }
        }
    }

    private String buildEnvelope(OutboxEvent row) {
        // In-house envelope shape matches the platform contract:
        // { eventId, eventType, aggregateId, partitionKey, createdAt, payload }.
        // payload is already a serialized JSON string; embed as-is.
        return "{\"eventId\":" + sequence.incrementAndGet()
                + ",\"eventType\":\"" + row.getEventType() + "\""
                + ",\"aggregateId\":\"" + row.getAggregateId() + "\""
                + ",\"partitionKey\":\"" + row.getPartitionKey() + "\""
                + ",\"createdAt\":\"" + row.getCreatedAt() + "\""
                + ",\"payload\":" + row.getPayload()
                + "}";
    }
}
