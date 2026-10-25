-- Cover image of a goal, uploaded by the member (already compressed by the app; JPEG / PNG / WebP,
-- max 1 MB). In its own table so goal lists never read it; goal.image_updated_at tells the app
-- whether there is one and when to reload it.
CREATE TABLE wellbeing.goal_image (
    goal_id      varchar(255) NOT NULL REFERENCES wellbeing.goal (id),
    content      bytea        NOT NULL,
    content_type varchar(20)  NOT NULL,
    updated_at   timestamptz  NOT NULL,
    CONSTRAINT goal_image_pkey PRIMARY KEY (goal_id),
    CONSTRAINT goal_image_type_check CHECK (content_type IN ('image/jpeg', 'image/png', 'image/webp'))
);

ALTER TABLE wellbeing.goal ADD COLUMN image_updated_at timestamptz;
