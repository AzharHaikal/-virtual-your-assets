-- 1. Session tokens are now stored as SHA-256 (TokenUtil.hash). Rows written before hold the raw token
--    and can never match a hashed lookup, so they are removed: every member and admin logs in again once.
DELETE FROM idp.member_token;
DELETE FROM admin_idp.admin_token;

-- 2. SUPER_ADMIN has no data restriction: the setting that could hide member transactions from the
--    SUPER_ADMIN (seeded in V6) no longer exists.
DELETE FROM admin_idp.system_config WHERE config_key = 'privacy.member_transaction_detail.enabled';
