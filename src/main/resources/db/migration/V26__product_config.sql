-- Product configuration managed from the portal (Phase 3.2).

-- Announcements: shown as a banner in the app while active and sent once as an ANNOUNCEMENT notification.
CREATE TABLE admin_idp.announcement (
    id           varchar(255)  NOT NULL,
    title        varchar(120)  NOT NULL,
    body         varchar(1000) NOT NULL,
    language     varchar(3)    NOT NULL DEFAULT 'ALL',  -- ALL, ID or EN (member.language)
    status       varchar(12)   NOT NULL DEFAULT 'DRAFT',
    starts_at    timestamptz   NOT NULL,
    ends_at      timestamptz,                           -- NULL = until unpublished
    published_at timestamptz,
    delivered_at timestamptz,                           -- notifications sent to the members
    created_at   timestamptz,
    created_by   varchar(255),
    updated_at   timestamptz,
    modified_by  varchar(255),
    deleted_at   timestamptz,
    CONSTRAINT announcement_pkey PRIMARY KEY (id),
    CONSTRAINT announcement_language_check CHECK (language IN ('ALL', 'ID', 'EN')),
    CONSTRAINT announcement_status_check CHECK (status IN ('DRAFT', 'PUBLISHED', 'UNPUBLISHED')),
    CONSTRAINT announcement_period_check CHECK (ends_at IS NULL OR ends_at > starts_at)
);
CREATE INDEX idx_announcement_active ON admin_idp.announcement (starts_at, ends_at)
    WHERE status = 'PUBLISHED' AND deleted_at IS NULL;

-- Legal documents: every publish is a new version; the app and the landing page show the latest one.
CREATE TABLE admin_idp.legal_document (
    id           varchar(255) NOT NULL,
    doc_type     varchar(10)  NOT NULL,
    language     varchar(2)   NOT NULL,
    version      integer      NOT NULL,
    content      text         NOT NULL,
    published_at timestamptz  NOT NULL,
    published_by varchar(255) NOT NULL,
    CONSTRAINT legal_document_pkey PRIMARY KEY (id),
    CONSTRAINT uq_legal_document_version UNIQUE (doc_type, language, version),
    CONSTRAINT legal_document_type_check CHECK (doc_type IN ('PRIVACY', 'TERMS')),
    CONSTRAINT legal_document_language_check CHECK (language IN ('ID', 'EN'))
);

-- Maintenance mode and OTP channels (system_config keys; a missing row means the default).
INSERT INTO admin_idp.system_config (config_key, config_value, updated_at) VALUES
    ('maintenance.enabled', 'false', now() AT TIME ZONE 'UTC'),
    ('otp.email.enabled', 'true', now() AT TIME ZONE 'UTC'),
    ('otp.whatsapp.enabled', 'false', now() AT TIME ZONE 'UTC')
ON CONFLICT (config_key) DO NOTHING;
