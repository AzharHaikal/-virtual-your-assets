-- FCM registration tokens of the members' devices (Phase 2.6). A token belongs to one member at a time:
-- registering it again moves it to the member signed in on that device. Removed on logout.
CREATE TABLE notification.device_push_token (
    id           varchar(255) NOT NULL,
    member_id    varchar(255) NOT NULL,
    token        varchar(512) NOT NULL,
    platform     varchar(10)  NOT NULL,
    created_at   timestamptz  NOT NULL,
    last_seen_at timestamptz  NOT NULL,
    CONSTRAINT device_push_token_pkey PRIMARY KEY (id),
    CONSTRAINT uq_device_push_token_token UNIQUE (token),
    CONSTRAINT device_push_token_platform_check CHECK (platform IN ('ANDROID', 'IOS'))
);
CREATE INDEX idx_device_push_token_member ON notification.device_push_token (member_id);
