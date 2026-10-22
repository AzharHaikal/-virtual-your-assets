-- Baseline of every application table.
--
-- Until now the tables were created by Hibernate (ddl-auto=update) and Flyway only created the
-- schemas (V1) and patched existing tables (V2-V4, skipped on a brand-new database). From this
-- migration on Flyway owns the schema and Hibernate only validates it (ddl-auto=validate).
--
-- The DDL below is exactly what Hibernate generated for the V1 entities, including its constraint
-- names, so that a database built by Flyway alone is identical to the existing ones:
--   * existing database: every CREATE ... IF NOT EXISTS is a no-op;
--   * brand-new database: this creates all tables (V2-V4 were skipped there).
--
-- From now on: one new V{n}__*.sql per schema change, never edit an applied migration.

-- ---------------------------------------------------------------------------------------------
-- admin_idp
-- ---------------------------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS admin_idp.admin (
    id                    varchar(255) NOT NULL,
    created_at            timestamp(6),
    created_by            varchar(255),
    deleted_at            timestamp(6),
    modified_by           varchar(255),
    updated_at            timestamp(6),
    email                 varchar(50)  NOT NULL,
    failed_login_attempts integer,
    last_login_at         timestamp(6),
    locked_until          timestamp(6),
    name                  varchar(50)  NOT NULL,
    password              varchar(255) NOT NULL,
    phone_number          varchar(15)  NOT NULL,
    role                  varchar(255),
    status                varchar(255),
    CONSTRAINT admin_pkey PRIMARY KEY (id),
    CONSTRAINT ukc0r9atamxvbhjjvy5j8da1kam UNIQUE (email),
    CONSTRAINT ukema32g9ju0a46v3ovs4d2imte UNIQUE (phone_number)
);

CREATE TABLE IF NOT EXISTS admin_idp.admin_activity (
    id          varchar(255) NOT NULL,
    admin_id    varchar(255) NOT NULL,
    admin_name  varchar(50)  NOT NULL,
    created_at  timestamp(6),
    description varchar(255),
    device      varchar(255),
    event       varchar(255),
    ip_address  varchar(255),
    CONSTRAINT admin_activity_pkey PRIMARY KEY (id)
);

CREATE TABLE IF NOT EXISTS admin_idp.admin_token (
    admin_token_id           varchar(255) NOT NULL,
    access_token             varchar(255) NOT NULL,
    access_token_expired_at  timestamp(6),
    admin_id                 varchar(255) NOT NULL,
    created_at               timestamp(6),
    device_id                varchar(255),
    device_name              varchar(255),
    ip_address               varchar(255),
    refresh_token            varchar(255) NOT NULL,
    refresh_token_expired_at timestamp(6),
    CONSTRAINT admin_token_pkey PRIMARY KEY (admin_token_id),
    CONSTRAINT uk4l51dco0hh7jge6ahgv83txfj UNIQUE (access_token),
    CONSTRAINT uk9x9ay4cqnyh2cn8k15774k31y UNIQUE (refresh_token)
);

-- ---------------------------------------------------------------------------------------------
-- idp
-- ---------------------------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS idp.member (
    id                  varchar(255) NOT NULL,
    created_at          timestamp(6),
    created_by          varchar(255),
    deleted_at          timestamp(6),
    modified_by         varchar(255),
    updated_at          timestamp(6),
    email               varchar(50)  NOT NULL,
    failed_pin_attempts integer,
    first_name          varchar(50)  NOT NULL,
    last_name           varchar(50)  NOT NULL,
    otp_locked_until    timestamp(6),
    phone_number        varchar(15)  NOT NULL,
    pin                 varchar(255) NOT NULL,
    pin_locked_until    timestamp(6),
    status              varchar(255),
    CONSTRAINT member_pkey PRIMARY KEY (id),
    CONSTRAINT ukmbmcqelty0fbrvxp1q58dn57t UNIQUE (email),
    CONSTRAINT ukn2qryhkfoqeel6njfhrcq6k7u UNIQUE (phone_number)
);

CREATE TABLE IF NOT EXISTS idp.member_activity (
    id           varchar(255) NOT NULL,
    created_at   timestamp(6),
    created_by   varchar(255),
    deleted_at   timestamp(6),
    modified_by  varchar(255),
    updated_at   timestamp(6),
    description  varchar(255),
    event        varchar(255),
    phone_number varchar(15)  NOT NULL,
    CONSTRAINT member_activity_pkey PRIMARY KEY (id)
);

CREATE TABLE IF NOT EXISTS idp.member_contact_change (
    id               varchar(255) NOT NULL,
    created_at       timestamp(6),
    member_id        varchar(255) NOT NULL,
    new_email        varchar(50),
    new_phone_number varchar(15),
    CONSTRAINT member_contact_change_pkey PRIMARY KEY (id),
    CONSTRAINT uq_member_contact_change_member UNIQUE (member_id)
);

CREATE TABLE IF NOT EXISTS idp.member_otp (
    id           varchar(255) NOT NULL,
    attempts     integer      NOT NULL,
    created_at   timestamp(6),
    expired_at   timestamp(6) NOT NULL,
    otp_code     varchar(255) NOT NULL,
    otp_type     varchar(255),
    phone_number varchar(15)  NOT NULL,
    verified_at  timestamp(6),
    CONSTRAINT member_otp_pkey PRIMARY KEY (id)
);

CREATE TABLE IF NOT EXISTS idp.member_token (
    member_token_id          varchar(255) NOT NULL,
    access_token             varchar(255) NOT NULL,
    access_token_expired_at  timestamp(6),
    created_at               timestamp(6),
    device_id                varchar(255) NOT NULL,
    device_name              varchar(255) NOT NULL,
    ip_address               varchar(255) NOT NULL,
    refresh_token            varchar(255) NOT NULL,
    refresh_token_expired_at timestamp(6),
    user_id                  varchar(255) NOT NULL,
    CONSTRAINT member_token_pkey PRIMARY KEY (member_token_id),
    CONSTRAINT uk8whgxbq5mw6ekhsg29511bevr UNIQUE (access_token),
    CONSTRAINT ukqgtxax4qyi5n50f4e2c0m60ck UNIQUE (refresh_token)
);

CREATE TABLE IF NOT EXISTS idp.rate_limit_bucket (
    bucket_key          varchar(120) NOT NULL,
    hits                integer      NOT NULL,
    window_start_millis bigint       NOT NULL,
    CONSTRAINT rate_limit_bucket_pkey PRIMARY KEY (bucket_key)
);

-- ---------------------------------------------------------------------------------------------
-- transaction_finance
-- ---------------------------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS transaction_finance.transaction (
    id                   varchar(255)   NOT NULL,
    created_at           timestamp(6),
    created_by           varchar(255),
    deleted_at           timestamp(6),
    modified_by          varchar(255),
    updated_at           timestamp(6),
    amount               numeric(19, 2) NOT NULL,
    member_id            varchar(255)   NOT NULL,
    reference_number     varchar(50)    NOT NULL,
    status               varchar(50),
    transaction_category varchar(255),
    transaction_date     timestamp(6),
    transaction_desc     varchar(255)   NOT NULL,
    transaction_type     varchar(20),
    user_email           varchar(50)    NOT NULL,
    user_phone_number    varchar(15)    NOT NULL,
    CONSTRAINT transaction_pkey PRIMARY KEY (id),
    CONSTRAINT uk4ifxw9gpaqmbisy3balsbu7rn UNIQUE (reference_number)
);

-- ---------------------------------------------------------------------------------------------
-- wallet
-- ---------------------------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS wallet.member_wallet (
    id           varchar(255) NOT NULL,
    created_at   timestamp(6),
    created_by   varchar(255),
    deleted_at   timestamp(6),
    modified_by  varchar(255),
    updated_at   timestamp(6),
    member_id    varchar(255) NOT NULL,
    phone_number varchar(15)  NOT NULL,
    status       varchar(255),
    CONSTRAINT member_wallet_pkey PRIMARY KEY (id)
);

CREATE TABLE IF NOT EXISTS wallet.wallet_statement (
    id               varchar(255) NOT NULL,
    created_at       timestamp(6),
    created_by       varchar(255),
    deleted_at       timestamp(6),
    modified_by      varchar(255),
    updated_at       timestamp(6),
    balance          varchar(255),          -- AES-256-GCM ciphertext (BalanceEncryptConverter)
    member_wallet_id varchar(255) NOT NULL,
    phone_number     varchar(255) NOT NULL,
    total_expense    numeric(19, 2),
    total_income     numeric(19, 2),
    CONSTRAINT wallet_statement_pkey PRIMARY KEY (id),
    CONSTRAINT uk4w2wo5hcttwqq2leu2cawnavr UNIQUE (member_wallet_id)
);

CREATE TABLE IF NOT EXISTS wallet.wallet_statement_history (
    id                  varchar(255)   NOT NULL,
    created_at          timestamp(6),
    created_by          varchar(255),
    deleted_at          timestamp(6),
    modified_by         varchar(255),
    updated_at          timestamp(6),
    current_balance     numeric(19, 2) NOT NULL,
    expense             numeric(19, 2),
    income              numeric(19, 2),
    member_wallet_id    varchar(255)   NOT NULL,
    previous_balance    numeric(19, 2) NOT NULL,
    transaction_id      varchar(255)   NOT NULL,
    wallet_statement_id varchar(255)   NOT NULL,
    CONSTRAINT wallet_statement_history_pkey PRIMARY KEY (id)
);

-- ---------------------------------------------------------------------------------------------
-- Enum check constraints.
--
-- ddl-auto=update creates these once and never updates them, so on an existing database they can
-- miss enum values added later (e.g. OtpType.CHANGE_CONTACT). Recreate them from the current enums.
-- Adding an enum value from now on needs a migration that recreates the matching constraint.
-- ---------------------------------------------------------------------------------------------
ALTER TABLE admin_idp.admin DROP CONSTRAINT IF EXISTS admin_role_check;
ALTER TABLE admin_idp.admin ADD CONSTRAINT admin_role_check
    CHECK (role IN ('SUPER_ADMIN', 'ADMINISTRATOR', 'OPERATOR', 'VIEWER'));

ALTER TABLE admin_idp.admin DROP CONSTRAINT IF EXISTS admin_status_check;
ALTER TABLE admin_idp.admin ADD CONSTRAINT admin_status_check
    CHECK (status IN ('ACTIVE', 'INACTIVE', 'SUSPENDED', 'DELETED'));

ALTER TABLE idp.member DROP CONSTRAINT IF EXISTS member_status_check;
ALTER TABLE idp.member ADD CONSTRAINT member_status_check
    CHECK (status IN ('ACTIVE', 'INACTIVE', 'SUSPENDED', 'DELETED'));

ALTER TABLE idp.member_otp DROP CONSTRAINT IF EXISTS member_otp_otp_type_check;
ALTER TABLE idp.member_otp ADD CONSTRAINT member_otp_otp_type_check
    CHECK (otp_type IN ('REGISTER', 'FORGOT_PIN', 'LOGIN', 'CHANGE_CONTACT'));

ALTER TABLE transaction_finance.transaction DROP CONSTRAINT IF EXISTS transaction_status_check;
ALTER TABLE transaction_finance.transaction ADD CONSTRAINT transaction_status_check
    CHECK (status IN ('INQUIRY', 'SUSPECT', 'FAILED', 'DELETED', 'SUCCESS'));

ALTER TABLE transaction_finance.transaction DROP CONSTRAINT IF EXISTS transaction_transaction_type_check;
ALTER TABLE transaction_finance.transaction ADD CONSTRAINT transaction_transaction_type_check
    CHECK (transaction_type IN ('INCOME', 'EXPENSE'));

ALTER TABLE wallet.member_wallet DROP CONSTRAINT IF EXISTS member_wallet_status_check;
ALTER TABLE wallet.member_wallet ADD CONSTRAINT member_wallet_status_check
    CHECK (status IN ('ACTIVE', 'INACTIVE', 'SUSPENDED', 'DELETED'));

-- ---------------------------------------------------------------------------------------------
-- Indexes (same as V2; V2 skipped them on a brand-new database).
-- ---------------------------------------------------------------------------------------------
CREATE INDEX IF NOT EXISTS idx_admin_created_at                 ON admin_idp.admin (created_at);
CREATE INDEX IF NOT EXISTS idx_admin_token_admin_id             ON admin_idp.admin_token (admin_id);
CREATE INDEX IF NOT EXISTS idx_admin_activity_created_at        ON admin_idp.admin_activity (created_at);
CREATE INDEX IF NOT EXISTS idx_member_status_created_at         ON idp.member (status, created_at);
CREATE INDEX IF NOT EXISTS idx_member_created_at                ON idp.member (created_at);
CREATE INDEX IF NOT EXISTS idx_member_activity_phone_created_at ON idp.member_activity (phone_number, created_at);
CREATE INDEX IF NOT EXISTS idx_member_activity_created_at       ON idp.member_activity (created_at);
CREATE INDEX IF NOT EXISTS idx_member_otp_phone_type_created_at ON idp.member_otp (phone_number, otp_type, created_at);
CREATE INDEX IF NOT EXISTS idx_member_wallet_phone              ON wallet.member_wallet (phone_number);
CREATE INDEX IF NOT EXISTS idx_member_wallet_member_id          ON wallet.member_wallet (member_id);
CREATE INDEX IF NOT EXISTS idx_wallet_statement_phone           ON wallet.wallet_statement (phone_number);
CREATE INDEX IF NOT EXISTS idx_transaction_member_date          ON transaction_finance.transaction (member_id, transaction_date);
CREATE INDEX IF NOT EXISTS idx_transaction_date                 ON transaction_finance.transaction (transaction_date);
CREATE INDEX IF NOT EXISTS idx_transaction_type_status_date     ON transaction_finance.transaction (transaction_type, status, transaction_date);
CREATE INDEX IF NOT EXISTS idx_transaction_user_phone           ON transaction_finance.transaction (user_phone_number);
