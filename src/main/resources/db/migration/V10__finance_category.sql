-- Transaction categories as data: 9 built-in categories (member_id NULL, stable ids "default-<CODE>")
-- plus custom categories per member. transaction.transaction_category is kept (code of a built-in
-- category, or OTHERS_INCOME / OTHERS_EXPENSE for a custom one) for older apps and admin reports.
CREATE TABLE transaction_finance.finance_category (
    id          varchar(255) NOT NULL,
    member_id   varchar(255),               -- NULL = built-in, visible to everyone
    code        varchar(50),                -- built-in only: TransactionCategory name
    name        varchar(50)  NOT NULL,
    type        varchar(20)  NOT NULL,
    icon        varchar(40)  NOT NULL,
    color       varchar(7)   NOT NULL,
    position    integer      NOT NULL DEFAULT 0,
    created_at  timestamptz,
    created_by  varchar(255),
    updated_at  timestamptz,
    modified_by varchar(255),
    deleted_at  timestamptz,
    CONSTRAINT finance_category_pkey PRIMARY KEY (id),
    CONSTRAINT finance_category_type_check CHECK (type IN ('INCOME', 'EXPENSE')),
    CONSTRAINT uq_finance_category_code UNIQUE (code)
);

-- One active category per name and type for each member (and among the built-in ones).
CREATE UNIQUE INDEX uq_finance_category_name
    ON transaction_finance.finance_category (COALESCE(member_id, ''), type, lower(name))
    WHERE deleted_at IS NULL;
CREATE INDEX idx_finance_category_member ON transaction_finance.finance_category (member_id);

INSERT INTO transaction_finance.finance_category (id, code, name, type, icon, color, position, created_at, created_by) VALUES
    ('default-SALARY',              'SALARY',              'Salary',            'INCOME',  'work',          '#22C55E', 1, now(), 'system'),
    ('default-PASSIVE_INCOME',      'PASSIVE_INCOME',      'Passive Income',    'INCOME',  'trending_up',   '#22C55E', 2, now(), 'system'),
    ('default-INVESTMENT',          'INVESTMENT',          'Investment',        'INCOME',  'show_chart',    '#22C55E', 3, now(), 'system'),
    ('default-OTHERS_INCOME',       'OTHERS_INCOME',       'Other Income',      'INCOME',  'attach_money',  '#22C55E', 4, now(), 'system'),
    ('default-FOOD_AND_BEVERAGE',   'FOOD_AND_BEVERAGE',   'Food & Beverage',   'EXPENSE', 'restaurant',    '#EF4444', 1, now(), 'system'),
    ('default-TRANSPORTATION',      'TRANSPORTATION',      'Transportation',    'EXPENSE', 'directions_car','#EF4444', 2, now(), 'system'),
    ('default-UTILITIES_AND_BILLS', 'UTILITIES_AND_BILLS', 'Bills & Utilities', 'EXPENSE', 'receipt_long',  '#EF4444', 3, now(), 'system'),
    ('default-ENTERTAINMENT',       'ENTERTAINMENT',       'Entertainment',     'EXPENSE', 'movie',         '#EF4444', 4, now(), 'system'),
    ('default-OTHERS_EXPENSE',      'OTHERS_EXPENSE',      'Other Expense',     'EXPENSE', 'money_off',     '#EF4444', 5, now(), 'system');

-- Link every transaction to a category (unknown codes fall back to "Other" of their type).
ALTER TABLE transaction_finance.transaction ADD COLUMN category_id varchar(255);

UPDATE transaction_finance.transaction t
SET category_id = CASE
        WHEN t.transaction_category IN (SELECT code FROM transaction_finance.finance_category) THEN 'default-' || t.transaction_category
        WHEN t.transaction_type = 'INCOME' THEN 'default-OTHERS_INCOME'
        ELSE 'default-OTHERS_EXPENSE'
    END;

ALTER TABLE transaction_finance.transaction
    ADD CONSTRAINT fk_transaction_category FOREIGN KEY (category_id) REFERENCES transaction_finance.finance_category (id);
CREATE INDEX idx_transaction_member_category ON transaction_finance.transaction (member_id, category_id);
