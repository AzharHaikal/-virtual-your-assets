-- Brute-force lock-out state (PIN attempts / OTP lock for members, login attempts for admins).
--
-- Tables are created by Hibernate (ddl-auto=update), so on a brand-new database they do not
-- exist yet when Flyway runs (Hibernate then creates them with these columns from the
-- entities). On an existing database this adds the columns; it is idempotent and skipped
-- when a table is missing. All columns are nullable: null counters are treated as 0.
DO $$
BEGIN
    IF to_regclass('idp.member') IS NOT NULL THEN
        ALTER TABLE idp.member ADD COLUMN IF NOT EXISTS failed_pin_attempts integer;
        ALTER TABLE idp.member ADD COLUMN IF NOT EXISTS pin_locked_until timestamp;
        ALTER TABLE idp.member ADD COLUMN IF NOT EXISTS otp_locked_until timestamp;
    END IF;

    IF to_regclass('admin_idp.admin') IS NOT NULL THEN
        ALTER TABLE admin_idp.admin ADD COLUMN IF NOT EXISTS failed_login_attempts integer;
        ALTER TABLE admin_idp.admin ADD COLUMN IF NOT EXISTS locked_until timestamp;
    END IF;
END $$;
