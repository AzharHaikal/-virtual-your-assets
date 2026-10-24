-- Savings targets (Phase 2.2): a VALUE goal in IDR can be linked to the finance overview.
-- Putting money aside only raises current_value (no expense transaction is created).
ALTER TABLE wellbeing.goal
    ADD COLUMN linked_to_finance boolean NOT NULL DEFAULT false,
    ADD CONSTRAINT goal_linked_finance_check
        CHECK (NOT linked_to_finance OR (progress_type = 'VALUE' AND unit = 'IDR'));
