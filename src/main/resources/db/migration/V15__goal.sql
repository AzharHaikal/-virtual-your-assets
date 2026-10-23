-- Goals & milestones (Phase 1.8). Private to the member like mood and habits: no admin endpoint reads these tables.
CREATE TABLE wellbeing.goal (
    id            varchar(255)   NOT NULL,
    member_id     varchar(255)   NOT NULL,
    title         varchar(80)    NOT NULL,
    description   varchar(1000),
    category      varchar(20)    NOT NULL,
    icon          varchar(40)    NOT NULL,
    color         varchar(7)     NOT NULL,
    progress_type varchar(16)    NOT NULL,             -- MILESTONE = milestones done / total, VALUE = current / target
    target_value  numeric(19, 2),                      -- required for VALUE
    current_value numeric(19, 2) NOT NULL DEFAULT 0,
    unit          varchar(20),                         -- e.g. "IDR", "materi"
    due_date      date,                                -- the member's day; passing it never changes the status
    status        varchar(16)    NOT NULL DEFAULT 'ACTIVE',
    completed_at  timestamptz,
    created_at    timestamptz,
    created_by    varchar(255),
    updated_at    timestamptz,
    modified_by   varchar(255),
    deleted_at    timestamptz,
    CONSTRAINT goal_pkey PRIMARY KEY (id),
    CONSTRAINT goal_category_check CHECK (category IN ('FINANCE', 'HEALTH', 'CAREER', 'EDUCATION', 'PERSONAL', 'OTHER')),
    CONSTRAINT goal_progress_type_check CHECK (progress_type IN ('MILESTONE', 'VALUE')),
    CONSTRAINT goal_status_check CHECK (status IN ('ACTIVE', 'COMPLETED', 'PAUSED', 'CANCELLED')),
    CONSTRAINT goal_target_check CHECK (progress_type = 'MILESTONE' OR target_value > 0),
    CONSTRAINT goal_current_check CHECK (current_value >= 0)
);
CREATE INDEX idx_goal_member ON wellbeing.goal (member_id) WHERE deleted_at IS NULL;

CREATE TABLE wellbeing.goal_milestone (
    id           varchar(255) NOT NULL,
    goal_id      varchar(255) NOT NULL REFERENCES wellbeing.goal (id),
    title        varchar(120) NOT NULL,
    position     integer      NOT NULL,
    completed_at timestamptz,
    created_at   timestamptz  NOT NULL,
    CONSTRAINT goal_milestone_pkey PRIMARY KEY (id)
);
CREATE INDEX idx_goal_milestone_goal ON wellbeing.goal_milestone (goal_id, position);

-- A change of the value (delta + value after it) and/or a note.
CREATE TABLE wellbeing.goal_progress_log (
    id          varchar(255)   NOT NULL,
    goal_id     varchar(255)   NOT NULL REFERENCES wellbeing.goal (id),
    delta       numeric(19, 2),
    value_after numeric(19, 2),
    note        varchar(500),
    created_at  timestamptz    NOT NULL,
    CONSTRAINT goal_progress_log_pkey PRIMARY KEY (id),
    CONSTRAINT goal_progress_log_content_check CHECK (delta IS NOT NULL OR note IS NOT NULL)
);
CREATE INDEX idx_goal_progress_log_goal ON wellbeing.goal_progress_log (goal_id, created_at DESC);
