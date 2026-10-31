-- Login, OTP verification / resend and forgot-PIN find the member by e-mail through
-- MemberRepository.findByEmailIgnoreCase, which Hibernate renders as upper(email) = upper(?).
-- The unique index on email cannot serve that expression, so each of these requests read the whole
-- idp.member table (audit 2026-10-31: 9.5 ms at 20k members, growing with the member count).
-- This index matches the expression.
--
-- CONCURRENTLY: idp.member stays writable while the index is built. PostgreSQL cannot do that inside a
-- transaction, so Flyway runs this migration without one (spring.flyway.postgresql.transactional-lock=false
-- keeps Flyway's own lock from blocking it). An interrupted build leaves an INVALID index behind: the DROP
-- removes it, so after flyway repair the migration can simply run again.
DROP INDEX CONCURRENTLY IF EXISTS idp.idx_member_email_upper;
CREATE INDEX CONCURRENTLY idx_member_email_upper ON idp.member (upper(email));
