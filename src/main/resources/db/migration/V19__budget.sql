-- Monthly budget per expense category (Phase 2.1). The amount spent is never stored: it is summed from
-- the member's SUCCESS transactions of the period when read, so editing or deleting a transaction is
-- reflected right away.
CREATE TABLE transaction_finance.budget (
    id           varchar(255)   NOT NULL,
    member_id    varchar(255)   NOT NULL,
    category_id  varchar(255)   NOT NULL REFERENCES transaction_finance.finance_category (id),
    amount       numeric(19, 2) NOT NULL,
    period_type  varchar(16)    NOT NULL DEFAULT 'MONTHLY',
    period_start date           NOT NULL,              -- first day of the month (member's calendar)
    period_end   date           NOT NULL,              -- last day of the month
    created_at   timestamptz,
    created_by   varchar(255),
    updated_at   timestamptz,
    modified_by  varchar(255),
    deleted_at   timestamptz,
    CONSTRAINT budget_pkey PRIMARY KEY (id),
    CONSTRAINT budget_amount_check CHECK (amount > 0),
    CONSTRAINT budget_period_type_check CHECK (period_type IN ('MONTHLY')),
    CONSTRAINT budget_period_check CHECK (period_end >= period_start)
);

-- One active budget per member, category and month; a deleted one can be created again.
CREATE UNIQUE INDEX uq_budget_member_category_period
    ON transaction_finance.budget (member_id, category_id, period_start) WHERE deleted_at IS NULL;
CREATE INDEX idx_budget_member_period ON transaction_finance.budget (member_id, period_start) WHERE deleted_at IS NULL;
