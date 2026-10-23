-- Mood check-in & journal (Phase 1.6). Private to the member: no admin endpoint reads these tables,
-- and the journal text is stored encrypted (AES-256-GCM, same key as the wallet balance).
CREATE SCHEMA IF NOT EXISTS wellbeing;

CREATE TABLE wellbeing.mood_tag (
    id          varchar(255) NOT NULL,
    member_id   varchar(255),               -- NULL = built-in
    code        varchar(50),                -- built-in only (translated by the apps)
    name        varchar(40)  NOT NULL,
    position    integer      NOT NULL DEFAULT 0,
    created_at  timestamptz,
    created_by  varchar(255),
    updated_at  timestamptz,
    modified_by varchar(255),
    deleted_at  timestamptz,
    CONSTRAINT mood_tag_pkey PRIMARY KEY (id),
    CONSTRAINT uq_mood_tag_code UNIQUE (code)
);
CREATE UNIQUE INDEX uq_mood_tag_name
    ON wellbeing.mood_tag (COALESCE(member_id, ''), lower(name)) WHERE deleted_at IS NULL;

INSERT INTO wellbeing.mood_tag (id, code, name, position, created_at, created_by) VALUES
    ('mood-tag-GOOD_SLEEP',  'GOOD_SLEEP',  'Good sleep',   1,  now(), 'system'),
    ('mood-tag-FRIENDS',     'FRIENDS',     'Friends',      2,  now(), 'system'),
    ('mood-tag-FAMILY',      'FAMILY',      'Family',       3,  now(), 'system'),
    ('mood-tag-WORK',        'WORK',        'Work',         4,  now(), 'system'),
    ('mood-tag-EXERCISE',    'EXERCISE',    'Exercise',     5,  now(), 'system'),
    ('mood-tag-GOOD_FOOD',   'GOOD_FOOD',   'Good food',    6,  now(), 'system'),
    ('mood-tag-HOBBY',       'HOBBY',       'Hobby',        7,  now(), 'system'),
    ('mood-tag-ACHIEVEMENT', 'ACHIEVEMENT', 'Achievement',  8,  now(), 'system'),
    ('mood-tag-STRESS',      'STRESS',      'Stress',       9,  now(), 'system'),
    ('mood-tag-TIRED',       'TIRED',       'Tired',        10, now(), 'system'),
    ('mood-tag-SICK',        'SICK',        'Sick',         11, now(), 'system'),
    ('mood-tag-MONEY',       'MONEY',       'Money',        12, now(), 'system');

CREATE TABLE wellbeing.mood_entry (
    id            varchar(255) NOT NULL,
    member_id     varchar(255) NOT NULL,
    local_date    date         NOT NULL,     -- the member's day, fixed when written
    mood_score    smallint     NOT NULL,     -- 1 (very bad) .. 5 (very good)
    note          text,                      -- encrypted journal text
    checked_in_at timestamptz  NOT NULL,
    created_at    timestamptz,
    created_by    varchar(255),
    updated_at    timestamptz,
    modified_by   varchar(255),
    deleted_at    timestamptz,
    CONSTRAINT mood_entry_pkey PRIMARY KEY (id),
    CONSTRAINT mood_entry_score_check CHECK (mood_score BETWEEN 1 AND 5)
);
-- One main entry per member and local date.
CREATE UNIQUE INDEX uq_mood_entry_member_date ON wellbeing.mood_entry (member_id, local_date) WHERE deleted_at IS NULL;

CREATE TABLE wellbeing.mood_entry_tag (
    mood_entry_id varchar(255) NOT NULL REFERENCES wellbeing.mood_entry (id) ON DELETE CASCADE,
    mood_tag_id   varchar(255) NOT NULL REFERENCES wellbeing.mood_tag (id),
    CONSTRAINT mood_entry_tag_pkey PRIMARY KEY (mood_entry_id, mood_tag_id)
);
