-- Habit tracker (Phase 1.7). Private to the member like mood: no admin endpoint reads these tables.
CREATE TABLE wellbeing.habit (
    id            varchar(255) NOT NULL,
    member_id     varchar(255) NOT NULL,
    name          varchar(60)  NOT NULL,
    category      varchar(20)  NOT NULL,
    icon          varchar(40)  NOT NULL,
    color         varchar(7)   NOT NULL,
    target_count  integer      NOT NULL DEFAULT 1,     -- 1 = done/not done, more = e.g. 8 glasses
    schedule_days smallint     NOT NULL DEFAULT 127,   -- bit 0 = Monday .. bit 6 = Sunday; 127 = every day
    start_date    date         NOT NULL,               -- the member's day the habit was created
    archived_at   timestamptz,
    created_at    timestamptz,
    created_by    varchar(255),
    updated_at    timestamptz,
    modified_by   varchar(255),
    deleted_at    timestamptz,
    CONSTRAINT habit_pkey PRIMARY KEY (id),
    CONSTRAINT habit_category_check CHECK (category IN ('PRODUCTIVITY', 'HEALTH', 'PERSONAL', 'OTHER')),
    CONSTRAINT habit_target_check CHECK (target_count BETWEEN 1 AND 100),
    CONSTRAINT habit_schedule_check CHECK (schedule_days BETWEEN 1 AND 127)
);
CREATE INDEX idx_habit_member ON wellbeing.habit (member_id) WHERE deleted_at IS NULL;

CREATE TABLE wellbeing.habit_completion (
    id           varchar(255) NOT NULL,
    habit_id     varchar(255) NOT NULL REFERENCES wellbeing.habit (id),
    member_id    varchar(255) NOT NULL,
    local_date   date         NOT NULL,                -- the member's day
    count        integer      NOT NULL,
    completed_at timestamptz  NOT NULL,
    CONSTRAINT habit_completion_pkey PRIMARY KEY (id),
    CONSTRAINT uq_habit_completion_habit_date UNIQUE (habit_id, local_date),
    CONSTRAINT habit_completion_count_check CHECK (count >= 1)
);
CREATE INDEX idx_habit_completion_member_date ON wellbeing.habit_completion (member_id, local_date);
