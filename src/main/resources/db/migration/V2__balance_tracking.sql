-- Balance state lives here, in account-service. One row per account.
-- Version supports optimistic concurrency for concurrent posting application.
-- amount stored as NUMERIC(19,4) — up to 4 fractional digits per the platform
-- contract; 19 total digits handles up to ~10^15 units, comfortably beyond
-- any single-account balance.

CREATE TABLE balance (
    account_id  VARCHAR(64)     PRIMARY KEY REFERENCES account (id),
    amount      NUMERIC(19,4)   NOT NULL DEFAULT 0,
    updated_at  TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    version     BIGINT          NOT NULL DEFAULT 0
);

-- Dedup for Kafka event processing. `idempotency_key` is the caller-supplied
-- key that flows from posting-manager through posting.transaction.received
-- into this service. A row here means: this posting has already been applied;
-- retries must skip.
CREATE TABLE processed_event (
    idempotency_key   VARCHAR(128)   PRIMARY KEY,
    posting_id        UUID           NOT NULL,
    processed_at      TIMESTAMPTZ    NOT NULL DEFAULT NOW()
);

-- Transactional outbox for account.balance.changed events. Written in the same
-- DB tx as the balance mutation; a scheduled publisher drains to Kafka. Same
-- pattern as posting-manager used before the thin-orchestrator refactor —
-- correct here because account-service is the money-mover and the write it
-- must be atomic with the event.
CREATE TABLE outbox_event (
    id            BIGSERIAL      PRIMARY KEY,
    event_type    VARCHAR(64)    NOT NULL,
    aggregate_id  UUID           NOT NULL,
    partition_key VARCHAR(128)   NOT NULL,
    payload       JSONB          NOT NULL,
    created_at    TIMESTAMPTZ    NOT NULL DEFAULT NOW(),
    sent_at       TIMESTAMPTZ,
    attempts      INT            NOT NULL DEFAULT 0
);

CREATE INDEX idx_outbox_unsent ON outbox_event (created_at) WHERE sent_at IS NULL;
