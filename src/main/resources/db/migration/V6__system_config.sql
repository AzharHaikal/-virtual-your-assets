-- Portal-managed settings (keys defined by SystemConfigKey).
CREATE TABLE admin_idp.system_config (
    config_key   varchar(100) NOT NULL,
    config_value varchar(500) NOT NULL,
    updated_by   varchar(255),
    updated_at   timestamp(6),
    CONSTRAINT system_config_pkey PRIMARY KEY (config_key)
);

-- Super Admin may view member-level transactions and balances (can be switched off in Settings).
INSERT INTO admin_idp.system_config (config_key, config_value, updated_at)
VALUES ('privacy.member_transaction_detail.enabled', 'true', now() AT TIME ZONE 'UTC');
