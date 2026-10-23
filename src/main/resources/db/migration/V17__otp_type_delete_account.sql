-- Self-service account deletion is confirmed with an OTP of its own type (Phase 1.11).
ALTER TABLE idp.member_otp DROP CONSTRAINT IF EXISTS member_otp_otp_type_check;
ALTER TABLE idp.member_otp ADD CONSTRAINT member_otp_otp_type_check
    CHECK (otp_type IN ('REGISTER', 'FORGOT_PIN', 'LOGIN', 'CHANGE_CONTACT', 'DELETE_ACCOUNT'));
