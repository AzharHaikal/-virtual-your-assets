-- Idempotency-Key for requests that must not run twice when the app retries (create transaction).
-- A row is reserved before the work starts (response_body NULL = in progress) and gets the
-- response afterwards; the same key returns that stored response. Rows expire after 24 hours.
CREATE TABLE transaction_finance.idempotency_key (
    id            varchar(200) NOT NULL,   -- member_id + ':' + key
    member_id     varchar(255) NOT NULL,
    request_hash  varchar(64)  NOT NULL,   -- SHA-256 of the request body
    response_body text,
    created_at    timestamptz  NOT NULL,
    CONSTRAINT idempotency_key_pkey PRIMARY KEY (id)
);

CREATE INDEX idx_idempotency_key_member_created ON transaction_finance.idempotency_key (member_id, created_at);
