-- In-app notifications and preferences (Phase 2.4). Private to the member: no admin endpoint reads them.
CREATE SCHEMA IF NOT EXISTS notification;

-- The inbox; the source of truth for every channel. dedup_key makes creating one idempotent
-- (e.g. HABIT:{habitId}:{localDate}), so a job that runs twice never notifies twice.
CREATE TABLE notification.notification (
    id         varchar(255) NOT NULL,
    member_id  varchar(255) NOT NULL,
    type       varchar(30)  NOT NULL,
    title      varchar(120) NOT NULL,              -- may contain habit/goal names: never logged
    body       varchar(500) NOT NULL,
    payload    jsonb,                              -- deep link for the app, e.g. {"route": "habit", "habitId": "..."}
    dedup_key  varchar(200) NOT NULL,
    read_at    timestamptz,
    created_at timestamptz  NOT NULL,
    CONSTRAINT notification_pkey PRIMARY KEY (id),
    CONSTRAINT uq_notification_dedup_key UNIQUE (dedup_key),
    CONSTRAINT notification_type_check
        CHECK (type IN ('HABIT_REMINDER', 'BUDGET_ALERT', 'GOAL_DUE', 'SYSTEM', 'ANNOUNCEMENT'))
);
CREATE INDEX idx_notification_member_created ON notification.notification (member_id, created_at DESC);
CREATE INDEX idx_notification_member_unread ON notification.notification (member_id) WHERE read_at IS NULL;

-- Per type and channel; a missing row means the default (IN_APP and PUSH on, EMAIL off).
-- Quiet hours are the member's local time and may pass midnight (22:00-07:00); the app sets the same
-- quiet hours on every row of the member.
CREATE TABLE notification.notification_preference (
    member_id   varchar(255) NOT NULL,
    type        varchar(30)  NOT NULL,
    channel     varchar(10)  NOT NULL,
    enabled     boolean      NOT NULL,
    quiet_start time,
    quiet_end   time,
    updated_at  timestamptz,
    CONSTRAINT notification_preference_pkey PRIMARY KEY (member_id, type, channel),
    CONSTRAINT notification_preference_type_check
        CHECK (type IN ('HABIT_REMINDER', 'BUDGET_ALERT', 'GOAL_DUE', 'SYSTEM', 'ANNOUNCEMENT')),
    CONSTRAINT notification_preference_channel_check CHECK (channel IN ('IN_APP', 'PUSH', 'EMAIL')),
    CONSTRAINT notification_preference_quiet_check CHECK ((quiet_start IS NULL) = (quiet_end IS NULL))
);

-- Delivery of a notification outside the app (push). A delivery that falls in quiet hours waits until
-- they end instead of being dropped; one that is no longer relevant by then is SKIPPED.
CREATE TABLE notification.notification_delivery (
    id              varchar(255) NOT NULL,
    notification_id varchar(255) NOT NULL REFERENCES notification.notification (id) ON DELETE CASCADE,
    channel         varchar(10)  NOT NULL,
    status          varchar(12)  NOT NULL,
    deliver_after   timestamptz  NOT NULL,
    attempts        integer      NOT NULL DEFAULT 0,
    sent_at         timestamptz,
    created_at      timestamptz  NOT NULL,
    CONSTRAINT notification_delivery_pkey PRIMARY KEY (id),
    CONSTRAINT notification_delivery_channel_check CHECK (channel IN ('PUSH', 'EMAIL')),
    CONSTRAINT notification_delivery_status_check CHECK (status IN ('PENDING', 'SENT', 'SKIPPED', 'FAILED'))
);
CREATE INDEX idx_notification_delivery_due ON notification.notification_delivery (deliver_after) WHERE status = 'PENDING';

-- ShedLock: every scheduled job (reminders, push, exports) runs on one instance at a time.
CREATE TABLE notification.shedlock (
    name       varchar(64)  NOT NULL,
    lock_until timestamptz  NOT NULL,
    locked_at  timestamptz  NOT NULL,
    locked_by  varchar(255) NOT NULL,
    CONSTRAINT shedlock_pkey PRIMARY KEY (name)
);
