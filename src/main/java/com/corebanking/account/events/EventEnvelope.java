package com.corebanking.account.events;

import tools.jackson.databind.JsonNode;

import java.time.Instant;
import java.util.UUID;

public record EventEnvelope(
        long eventId,
        String eventType,
        UUID aggregateId,
        String partitionKey,
        Instant createdAt,
        JsonNode payload
) {
}
