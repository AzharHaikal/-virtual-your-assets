-- Time zone support.
--
-- From now on every instant is stored as timestamptz (UTC) and the application runs in UTC
-- (TimeService). Day-based data keeps the member's local date (transaction.local_date), fixed
-- when the row is written, so a later time zone change never moves past days.
--
-- Values written before this migration were "timestamp without time zone" in two zones:
--   * created_at / updated_at / ... : written by the server JVM   -> ${legacyServerZone}
--   * transaction.transaction_date  : sent by the app, device time -> ${legacyClientZone}
-- (placeholders: spring.flyway.placeholders, env VYRA_LEGACY_SERVER_TIMEZONE / VYRA_LEGACY_CLIENT_TIMEZONE)

-- 1. Member time zone (IANA id).
ALTER TABLE idp.member ADD COLUMN timezone varchar(64) NOT NULL DEFAULT 'Asia/Jakarta';

-- 2. Transaction date: app local time -> UTC instant.
ALTER TABLE transaction_finance.transaction
    ALTER COLUMN transaction_date TYPE timestamptz USING transaction_date AT TIME ZONE '${legacyClientZone}';

-- 3. Every other timestamp column of the application schemas: server time -> UTC instant.
DO $$
DECLARE
    col record;
BEGIN
    FOR col IN
        SELECT table_schema, table_name, column_name
        FROM information_schema.columns
        WHERE table_schema IN ('admin_idp', 'idp', 'transaction_finance', 'wallet')
          AND data_type = 'timestamp without time zone'
    LOOP
        EXECUTE format('ALTER TABLE %I.%I ALTER COLUMN %I TYPE timestamptz USING %I AT TIME ZONE %L',
                       col.table_schema, col.table_name, col.column_name, col.column_name,
                       '${legacyServerZone}');
    END LOOP;
END $$;

-- 4. Member-local date of each transaction (grouping, filters, charts).
ALTER TABLE transaction_finance.transaction ADD COLUMN local_date date;

UPDATE transaction_finance.transaction
SET local_date = (COALESCE(transaction_date, created_at, now()) AT TIME ZONE '${legacyClientZone}')::date;

ALTER TABLE transaction_finance.transaction ALTER COLUMN local_date SET NOT NULL;

CREATE INDEX idx_transaction_member_local_date ON transaction_finance.transaction (member_id, local_date);
CREATE INDEX idx_transaction_local_date        ON transaction_finance.transaction (local_date);
