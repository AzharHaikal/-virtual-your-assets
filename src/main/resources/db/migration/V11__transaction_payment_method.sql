-- Optional payment method of a transaction (Phase 1.4).
ALTER TABLE transaction_finance.transaction ADD COLUMN payment_method varchar(20);

ALTER TABLE transaction_finance.transaction
    ADD CONSTRAINT transaction_payment_method_check
    CHECK (payment_method IN ('CASH', 'TRANSFER', 'E_WALLET', 'CARD', 'OTHER'));
