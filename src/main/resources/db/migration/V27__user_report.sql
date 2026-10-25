-- Problems and abuse reported by members (Phase 3.4). Handling a report never opens the member's
-- private data (mood, journal, habits, goals); transaction details keep the SUPER_ADMIN rule.
CREATE TABLE idp.user_report (
    id                varchar(255)  NOT NULL,
    member_id         varchar(255)  NOT NULL,
    category          varchar(20)   NOT NULL,
    description       varchar(2000) NOT NULL,
    has_attachment    boolean       NOT NULL DEFAULT false,
    app_version       varchar(30),
    platform          varchar(20),
    status            varchar(12)   NOT NULL DEFAULT 'OPEN',
    assigned_admin_id varchar(255),
    resolved_at       timestamptz,
    created_at        timestamptz   NOT NULL,
    updated_at        timestamptz,
    CONSTRAINT user_report_pkey PRIMARY KEY (id),
    CONSTRAINT user_report_category_check CHECK (category IN ('BUG', 'ACCOUNT', 'ABUSE', 'SUGGESTION', 'OTHER')),
    CONSTRAINT user_report_status_check CHECK (status IN ('OPEN', 'IN_PROGRESS', 'RESOLVED'))
);
CREATE INDEX idx_user_report_status ON idp.user_report (status, created_at DESC);
CREATE INDEX idx_user_report_member ON idp.user_report (member_id, created_at DESC);

-- Optional screenshot (JPEG / PNG / WebP, max 1 MB), in its own table so listing reports never reads it.
CREATE TABLE idp.user_report_attachment (
    report_id    varchar(255) NOT NULL REFERENCES idp.user_report (id),
    content      bytea        NOT NULL,
    content_type varchar(20)  NOT NULL,
    CONSTRAINT user_report_attachment_pkey PRIMARY KEY (report_id),
    CONSTRAINT user_report_attachment_type_check CHECK (content_type IN ('image/jpeg', 'image/png', 'image/webp'))
);

-- What the admins did with a report (status changes, assignment, notes), oldest first.
CREATE TABLE idp.user_report_note (
    id         varchar(255)  NOT NULL,
    report_id  varchar(255)  NOT NULL REFERENCES idp.user_report (id),
    admin_id   varchar(255)  NOT NULL,
    status     varchar(12),                             -- the new status, when it changed
    note       varchar(1000),
    created_at timestamptz   NOT NULL,
    CONSTRAINT user_report_note_pkey PRIMARY KEY (id)
);
CREATE INDEX idx_user_report_note_report ON idp.user_report_note (report_id, created_at);
