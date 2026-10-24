-- Export of all the member's personal data (Phase 2.7), built in the background as a ZIP.
-- The file is kept here (like the profile photo) for 24 hours, then emptied. The e-mailed download link
-- carries a random token; only its SHA-256 is stored.
CREATE TABLE idp.data_export (
    id                  varchar(255) NOT NULL,
    member_id           varchar(255) NOT NULL,
    status              varchar(12)  NOT NULL,
    requested_at        timestamptz  NOT NULL,
    started_at          timestamptz,
    completed_at        timestamptz,
    expires_at          timestamptz,
    download_token_hash varchar(64),
    content             bytea,
    size_bytes          bigint,
    error               varchar(255),              -- technical reason only, never personal data
    CONSTRAINT data_export_pkey PRIMARY KEY (id),
    CONSTRAINT data_export_status_check CHECK (status IN ('PENDING', 'PROCESSING', 'READY', 'FAILED', 'EXPIRED'))
);
CREATE INDEX idx_data_export_member_requested ON idp.data_export (member_id, requested_at DESC);
CREATE INDEX idx_data_export_status ON idp.data_export (status);
