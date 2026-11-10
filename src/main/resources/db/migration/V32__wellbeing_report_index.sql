-- Portal Mood report (SUPER_ADMIN): check-ins of every member for a range of days. The existing
-- unique index starts with member_id, so a platform-wide range of days needs its own index.
CREATE INDEX IF NOT EXISTS idx_mood_entry_local_date ON wellbeing.mood_entry (local_date) WHERE deleted_at IS NULL;
