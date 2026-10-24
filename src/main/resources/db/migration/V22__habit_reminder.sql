-- Habit reminders (Phase 2.5): a local time of day in the member's zone.
ALTER TABLE wellbeing.habit
    ADD COLUMN reminder_time    time,
    ADD COLUMN reminder_enabled boolean NOT NULL DEFAULT false,
    ADD CONSTRAINT habit_reminder_check CHECK (NOT reminder_enabled OR reminder_time IS NOT NULL);

CREATE INDEX idx_habit_reminder ON wellbeing.habit (reminder_time)
    WHERE reminder_enabled AND archived_at IS NULL AND deleted_at IS NULL;
