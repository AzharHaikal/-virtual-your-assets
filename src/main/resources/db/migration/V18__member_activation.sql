-- First time a member did each core action (activation metrics, PRD §13; Phase 2). Written once per event.
CREATE TABLE idp.member_activation (
    member_id   varchar(255) NOT NULL,
    event       varchar(40)  NOT NULL,
    occurred_at timestamptz  NOT NULL,
    CONSTRAINT member_activation_pkey PRIMARY KEY (member_id, event),
    CONSTRAINT member_activation_event_check CHECK (event IN ('ONBOARDING_COMPLETED', 'FIRST_TRANSACTION',
        'FIRST_MOOD', 'FIRST_HABIT_COMPLETION', 'FIRST_GOAL', 'FIRST_BUDGET'))
);
