-- Edit / delete of transactions (Phase 1). The wallet ledger stays append-only: an edit or a
-- delete adds a correction row instead of rewriting the original one.
ALTER TABLE wallet.wallet_statement_history
    ADD COLUMN entry_type varchar(16) NOT NULL DEFAULT 'CREATE';

ALTER TABLE wallet.wallet_statement_history
    ADD CONSTRAINT wallet_statement_history_entry_type_check CHECK (entry_type IN ('CREATE', 'UPDATE', 'DELETE'));

CREATE INDEX idx_wallet_statement_history_transaction ON wallet.wallet_statement_history (transaction_id);
