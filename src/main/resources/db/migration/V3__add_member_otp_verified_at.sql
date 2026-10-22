-- FORGOT_PIN OTP verification marker used by /auth/reset-pin.
--
-- Tables are created by Hibernate (ddl-auto=update), so on a brand-new database the table
-- does not exist yet when Flyway runs (Hibernate then creates it with the column from the
-- entity). On an existing database this adds the column; it is idempotent and skipped when
-- the table is missing.
--
-- Existing rows keep verified_at = NULL, i.e. they can no longer be used to reset a PIN:
-- an in-flight reset simply has to verify its OTP again.
DO $$
BEGIN
    IF to_regclass('idp.member_otp') IS NOT NULL THEN
        ALTER TABLE idp.member_otp ADD COLUMN IF NOT EXISTS verified_at timestamp;
    END IF;
END $$;
