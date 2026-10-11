-- Performance indexes for the hottest query paths.
--
-- Tables are created by Hibernate (ddl-auto=update), so on a brand-new database the
-- tables do not exist yet when Flyway runs; the same indexes are declared on the JPA
-- entities (@Table(indexes = ...)) and are created by Hibernate there. This migration
-- covers existing databases and is idempotent (IF NOT EXISTS, skipped when the table
-- is missing).
DO $$
DECLARE
    spec record;
BEGIN
    FOR spec IN
        SELECT * FROM (VALUES
            -- admin portal: lists, audit trail, token cleanup
            ('idx_admin_created_at',                 'admin_idp.admin',                      'created_at'),
            ('idx_admin_token_admin_id',             'admin_idp.admin_token',                'admin_id'),
            ('idx_admin_activity_created_at',        'admin_idp.admin_activity',             'created_at'),
            -- members: list/filter by status, newest first, dashboard counters
            ('idx_member_status_created_at',         'idp.member',                           'status, created_at'),
            ('idx_member_created_at',                'idp.member',                           'created_at'),
            -- member audit trail + per-member activity feed
            ('idx_member_activity_phone_created_at', 'idp.member_activity',                  'phone_number, created_at'),
            ('idx_member_activity_created_at',       'idp.member_activity',                  'created_at'),
            -- latest OTP lookup
            ('idx_member_otp_phone_type_created_at', 'idp.member_otp',                       'phone_number, otp_type, created_at'),
            -- wallet lookups by phone / member
            ('idx_member_wallet_phone',              'wallet.member_wallet',                 'phone_number'),
            ('idx_member_wallet_member_id',          'wallet.member_wallet',                 'member_id'),
            ('idx_wallet_statement_phone',           'wallet.wallet_statement',              'phone_number'),
            -- transactions: member history, platform reports, income/expense aggregation
            ('idx_transaction_member_date',          'transaction_finance.transaction',      'member_id, transaction_date'),
            ('idx_transaction_date',                 'transaction_finance.transaction',      'transaction_date'),
            ('idx_transaction_type_status_date',     'transaction_finance.transaction',      'transaction_type, status, transaction_date'),
            ('idx_transaction_user_phone',           'transaction_finance.transaction',      'user_phone_number')
        ) AS t(index_name, table_name, column_list)
    LOOP
        IF to_regclass(spec.table_name) IS NOT NULL THEN
            EXECUTE format('CREATE INDEX IF NOT EXISTS %I ON %s (%s)',
                           spec.index_name, spec.table_name, spec.column_list);
        END IF;
    END LOOP;
END $$;
