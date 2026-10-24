-- Onboarding and a personal home dashboard (Phase 2.8), kept on the server so every device shows the same.
ALTER TABLE idp.member
    ADD COLUMN onboarding_completed_at timestamptz,
    ADD COLUMN enabled_modules         varchar(100),   -- comma separated FINANCE,MOOD,HABIT,GOAL; NULL = all
    ADD COLUMN dashboard_layout        jsonb;          -- [{"card": "BALANCE", "hidden": false}, ...] in order; NULL = default
