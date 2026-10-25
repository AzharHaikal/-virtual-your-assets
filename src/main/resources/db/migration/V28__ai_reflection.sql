-- Explicit consents of a member (Phase 3.5), e.g. AI_REFLECTION. Revoking keeps the row with revoked_at.
CREATE TABLE idp.member_consent (
    member_id    varchar(255) NOT NULL,
    consent_type varchar(30)  NOT NULL,
    granted_at   timestamptz,
    revoked_at   timestamptz,
    updated_at   timestamptz  NOT NULL,
    CONSTRAINT member_consent_pkey PRIMARY KEY (member_id, consent_type),
    CONSTRAINT member_consent_type_check CHECK (consent_type IN ('AI_REFLECTION'))
);

-- Weekly AI reflections: the member's own text, encrypted like the journal, deletable at any time.
CREATE TABLE wellbeing.ai_reflection (
    id           varchar(255) NOT NULL,
    member_id    varchar(255) NOT NULL,
    period_start date         NOT NULL,             -- Monday of the week (member's days)
    period_end   date         NOT NULL,
    content      text         NOT NULL,             -- encrypted
    model        varchar(60)  NOT NULL,
    created_at   timestamptz  NOT NULL,
    deleted_at   timestamptz,
    CONSTRAINT ai_reflection_pkey PRIMARY KEY (id)
);
CREATE UNIQUE INDEX uq_ai_reflection_member_week
    ON wellbeing.ai_reflection (member_id, period_start) WHERE deleted_at IS NULL;
