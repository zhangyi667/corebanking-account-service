-- Account is a financial identity: a currency-scoped ledger position.
-- Owned by an opaque `owner_id` — the user/customer domain is a separate service.
--
-- status transitions:
--   ACTIVE  -> FROZEN  (manual, e.g. compliance hold)
--   ACTIVE  -> CLOSED  (permanent)
--   FROZEN  -> ACTIVE  (manual)
--   FROZEN  -> CLOSED  (permanent)
--   CLOSED  is terminal
--
-- version supports optimistic concurrency for status updates.

CREATE TABLE account (
    id          VARCHAR(64)   PRIMARY KEY,
    owner_id    VARCHAR(64)   NOT NULL,
    currency    CHAR(3)       NOT NULL,
    status      VARCHAR(16)   NOT NULL,
    created_at  TIMESTAMPTZ   NOT NULL DEFAULT NOW(),
    version     BIGINT        NOT NULL DEFAULT 0,
    CONSTRAINT account_status_valid CHECK (status IN ('ACTIVE', 'FROZEN', 'CLOSED')),
    CONSTRAINT account_currency_iso CHECK (currency ~ '^[A-Z]{3}$')
);

CREATE INDEX idx_account_owner ON account (owner_id);
