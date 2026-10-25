-- Goal images are soft deleted like their goals: removing the image or deleting the goal marks the row
-- DELETED (deleted_at set) instead of removing it. A new upload for the same goal makes it ACTIVE again.
ALTER TABLE wellbeing.goal_image
    ADD COLUMN status     varchar(10) NOT NULL DEFAULT 'ACTIVE',
    ADD COLUMN deleted_at timestamptz,
    ADD CONSTRAINT goal_image_status_check CHECK (status IN ('ACTIVE', 'DELETED'));

-- Images of goals that were already deleted.
UPDATE wellbeing.goal_image i
SET status = 'DELETED', deleted_at = g.deleted_at
FROM wellbeing.goal g
WHERE g.id = i.goal_id AND g.deleted_at IS NOT NULL;
