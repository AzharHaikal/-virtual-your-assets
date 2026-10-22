package com.vyra.be_virtual_your_assets.constant;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum ErrorConstant {

    // Register
    REGISTER_SUCCESS("VYRA-REG-000", "Your registration was successful"),
    REGISTER_FAILED("VYRA-REG-001", "Your registration was failed"),

    CREATE_WALLET_SUCCESS("VYRA-WLT-000", "Create wallet was successful"),
    CREATE_WALLET_FAILED("VYRA-WLT-001", "Create wallet was failed"),

    EMAIL_SEND_FAILED("VYRA-EML-001", "Failed when sent otp via email"),

    LOGIN_SUCCESS("VYRA-LGN-000", "You've successfully logged in"),
    LOGOUT_SUCCESS("VYRA-LGT-000", "You've successfully logged out"),

    FORGOT_PIN_OTP_SENT("VYRA-FP-001", "An OTP has been sent"),
    RESET_PIN_SUCCESS("VYRA-FP-002", "Your PIN has been successfully updated"),

    // Token
    TOKEN_CREATED("VYRA-TKN-001", "Your session has been created"),
    TOKEN_EXPIRED("VYRA-TKN-002", "Your session has expired. Please log in again"),
    INVALID_REFRESH_TOKEN("VYRA-TKN-003", "Invalid refresh token"),
    REFRESH_TOKEN_EXPIRED("VYRA-TKN-004", "Your refresh session has expired. Please log in again"),
    REFRESH_TOKEN_SUCCESS("VYRA-TKN-005", "Access token refreshed successfully"),

    // Invalid
    INVALID_PIN("VYRA-INV-001", "Invalid PIN. Please try again"),
    INVALID_PIN_MISS_MATCH("VYRA-INV-002", "Wrong PIN. Please input with correct PIN"),
    INVALID_TIMEZONE("VYRA-INV-003", "Invalid time zone. Use a region id such as Asia/Jakarta"),
    INVALID_DATE_TIME("VYRA-INV-004", "Invalid date. Use ISO-8601, e.g. 2026-10-22T08:30:00+07:00"),

    // Not Found (generic to prevent enumeration)
    PHONE_NUMBER_NOT_FOUND("VYRA-DNF-001", "Invalid account. Please check your account details"),
    EMAIL_NOT_FOUND("VYRA-DNF-002", "Invalid account. Please check your account details"),
    MEMBER_NOT_FOUND("VYRA-DNF-003", "Invalid account. Please check your account details"),
    MEMBER_NOT_ACTIVE("VYRA-DNF-004", "Invalid account. Please check your account status"),
    MEMBER_SUSPENDED("VYRA-DNF-005", "Invalid account. Please check your account status"),

    // Exists (generic)
    PHONE_NUMBER_ALREADY_EXIST("VYRA-EXS-001", "Your account already exists. Please use a different account"),
    EMAIL_ALREADY_EXIST("VYRA-EXS-002", "Your account already exists. Please use a different account"),

    PHONE_NUMBER_ALREADY_EXIST_V2("VYRA-EXS-003", "Email already exists, please use different email"),
    EMAIL_ALREADY_EXIST_V2("VYRA-EXS-004", "Phone number already exists, please use different phone number"),

    // OTP
    VERIFY_OTP_SUCCESS("VYRA-OTP-000", "OTP verification successful"),
    RESEND_OTP("VYRA-OTP-001", "If the account exists, a new OTP has been sent"),
    OTP_NOT_FOUND("VYRA-OTP-002", "Invalid or expired OTP"),
    OTP_EXPIRED("VYRA-OTP-003", "The OTP has expired. Please request a new one"),
    OTP_INVALID("VYRA-OTP-004", "Invalid OTP. Please enter the OTP correctly. Remaining attempts - "),
    MAX_ATTEMPTS_REACHED("VYRA-OTP-005", "You've reached the maximum attempts. Verification is temporarily locked. Please try again in "),

    // Lock-out and rate limiting (HTTP 429). The remaining wait is appended to the message.
    ACCOUNT_LOCKED("VYRA-LCK-001", "Too many failed attempts. Your account is temporarily locked. Please try again in"),
    OTP_LOCKED("VYRA-LCK-002", "Too many invalid codes. Verification is temporarily locked. Please try again in"),
    TOO_MANY_REQUESTS("VYRA-RTL-001", "Too many requests. Please try again in"),
    OTP_COOLDOWN("VYRA-RTL-002", "A code was sent recently. Please wait before requesting another one. Try again in"),

    // Member
    GET_MEMBER_SUCCESS("VYRA-GMS-000", "Get member detail successful"),
    GET_MEMBER_ACTIVITY_SUCCESS("VYRA-GMA-000", "Get member activity successful"),
    GET_MEMBER_FAILED("VYRA-GMS-001", "Get member detail failed"),

    // Update member profile
    UPDATE_PROFILE_SUCCESS("VYRA-UPS-000", "Update member profile successful"),
    UPDATE_PROFILE_PENDING("VYRA-UPS-001", "A verification code has been sent. Enter it to confirm the change of your e-mail address or phone number"),
    UPDATE_WALLET_SUCCESS("VYRA-UWS-000", "Update member wallet successful"),
    UPDATE_WALLET_FAILED("VYRA-UWS-000", "Update member wallet failed"),

    // Member Wallet
    GET_MEMBER_WALLET_SUCCESS("VYRA-GWS-000", "Get member detail successful"),
    GET_MEMBER_WALLET_FAILED("VYRA-GWS-001", "Get member detail failed"),

    // Chart
    GET_CHART_SUCCESS("VYRA-GCS-000", "Get chart successful"),

    // Transaction Wallet
    CREATE_TRANSACTION_WALLET_SUCCESS("VYRA-TWS-000", "Create transaction wallet successful"),
    CREATE_TRANSACTION_WALLET_FAILED("VYRA-TWS-001", "Get member detail successful"),

    CREATE_TRANSACTION_SUCCESS("VYRA-CTS-000", "Create transaction successful"),
    UPDATE_TRANSACTION_SUCCESS("VYRA-UTS-000", "Transaction updated successfully"),
    DELETE_TRANSACTION_SUCCESS("VYRA-DTS-000", "Transaction deleted successfully"),
    TRANSACTION_NOT_FOUND("VYRA-TRX-404", "Transaction could not be found"),
    TRANSACTION_NOT_EDITABLE("VYRA-TRX-001", "Only successful transactions can be changed"),

    // Idempotency-Key
    IDEMPOTENCY_KEY_INVALID("VYRA-IDM-001", "Invalid Idempotency-Key (max 100 characters)"),
    IDEMPOTENCY_KEY_REUSED("VYRA-IDM-002", "This Idempotency-Key was already used for a different request"),
    IDEMPOTENCY_KEY_IN_PROGRESS("VYRA-IDM-003", "The same request is still being processed. Please wait a moment"),

    // Transaction History
    GET_TOP_TRANSACTION_HISTORY_SUCCESS("VYRA-GTT-000","Top transaction history retrieved successfully"),
    GET_TRANSACTION_HISTORY_SUCCESS("VYRA-GTH-000","Transaction history retrieved successfully"),

    // Member Change Pin
    CHANGE_PIN_SUCCESS("VYRA-CPS-000", "Change pin successful"),

    // Exception
    BAD_REQUEST("VYRA-ERR400", "Something went wrong with your request"),
    DATA_NOT_FOUND("VYRA-ERR404", "The requested data could not be found"),
    INTERNAL_SERVER_ERROR("VYRA-ERR500", "We are experiencing a system issue. Please try again later"),
    SYSTEM_BUSY("VYRA-ERR503", "The system is busy right now. Please try again in a moment"),

    // =========================================================================
    // ADMIN AUTH
    // =========================================================================
    ADMIN_LOGIN_SUCCESS("VYRA-ADM-LGN-000", "You've successfully logged in"),
    ADMIN_LOGOUT_SUCCESS("VYRA-ADM-LGT-000", "You've successfully logged out"),
    ADMIN_REFRESH_TOKEN_SUCCESS("VYRA-ADM-TKN-000", "Access token refreshed successfully"),
    ADMIN_INVALID_CREDENTIALS("VYRA-ADM-LGN-001", "Invalid email or password"),
    ADMIN_NOT_FOUND("VYRA-ADM-DNF-001", "Admin account could not be found"),
    ADMIN_NOT_ACTIVE("VYRA-ADM-LGN-002", "Your account is not active. Please contact a super administrator"),
    ADMIN_INVALID_REFRESH_TOKEN("VYRA-ADM-TKN-001", "Invalid refresh token"),
    ADMIN_REFRESH_TOKEN_EXPIRED("VYRA-ADM-TKN-002", "Your session has expired. Please log in again"),
    ADMIN_UNAUTHORIZED("VYRA-ADM-AUT-001", "You don't have permission to perform this action"),
    MEMBER_TRANSACTION_ACCESS_DISABLED("VYRA-ADM-AUT-002", "Access to member transaction details is turned off in Settings"),

    // =========================================================================
    // ADMIN SYSTEM SETTINGS
    // =========================================================================
    SYSTEM_CONFIG_LIST_SUCCESS("VYRA-CFG-LST-000", "Settings retrieved successfully"),
    SYSTEM_CONFIG_UPDATE_SUCCESS("VYRA-CFG-UPD-000", "Setting updated successfully"),
    SYSTEM_CONFIG_NOT_FOUND("VYRA-CFG-DNF-001", "Setting could not be found"),
    SYSTEM_CONFIG_INVALID_VALUE("VYRA-CFG-VAL-001", "Invalid value for this setting"),

    // =========================================================================
    // ADMIN MANAGEMENT (CRUD admin users / RBAC)
    // =========================================================================
    ADMIN_REGISTER_SUCCESS("VYRA-ADM-REG-000", "Admin account has been successfully created"),
    ADMIN_EMAIL_ALREADY_EXIST("VYRA-ADM-EXS-001", "An admin account with this email already exists"),
    ADMIN_PHONE_ALREADY_EXIST("VYRA-ADM-EXS-002", "An admin account with this phone number already exists"),
    ADMIN_GET_LIST_SUCCESS("VYRA-ADM-LST-000", "Admin list retrieved successfully"),
    ADMIN_GET_DETAIL_SUCCESS("VYRA-ADM-DTL-000", "Admin detail retrieved successfully"),
    ADMIN_UPDATE_SUCCESS("VYRA-ADM-UPD-000", "Admin account updated successfully"),
    ADMIN_DELETE_SUCCESS("VYRA-ADM-DEL-000", "Admin account deleted successfully"),
    ADMIN_UPDATE_STATUS_SUCCESS("VYRA-ADM-STS-000", "Admin status updated successfully"),
    ADMIN_RESET_PASSWORD_SUCCESS("VYRA-ADM-RPW-000", "Admin password has been reset successfully"),
    ADMIN_CANNOT_MODIFY_SELF_STATUS("VYRA-ADM-STS-001", "You cannot change the status of your own account"),
    ADMIN_CANNOT_DELETE_SELF("VYRA-ADM-DEL-001", "You cannot delete your own account"),
    ADMIN_GET_PROFILE_SUCCESS("VYRA-ADM-PRF-000", "Admin profile retrieved successfully"),

    // =========================================================================
    // ADMIN DASHBOARD
    // =========================================================================
    DASHBOARD_SUMMARY_SUCCESS("VYRA-DSH-SUM-000", "Dashboard summary retrieved successfully"),
    DASHBOARD_CHART_SUCCESS("VYRA-DSH-CHT-000", "Dashboard chart data retrieved successfully"),
    DASHBOARD_DISTRIBUTION_SUCCESS("VYRA-DSH-DST-000", "Category distribution retrieved successfully"),
    DASHBOARD_RECENT_TRANSACTIONS_SUCCESS("VYRA-DSH-TRX-000", "Recent transactions retrieved successfully"),
    DASHBOARD_LATEST_MEMBERS_SUCCESS("VYRA-DSH-MBR-000", "Latest members retrieved successfully"),

    // =========================================================================
    // ADMIN MEMBER MANAGEMENT
    // =========================================================================
    ADMIN_MEMBER_LIST_SUCCESS("VYRA-AMM-LST-000", "Member list retrieved successfully"),
    ADMIN_MEMBER_DETAIL_SUCCESS("VYRA-AMM-DTL-000", "Member detail retrieved successfully"),
    ADMIN_MEMBER_TRANSACTIONS_SUCCESS("VYRA-AMM-TRX-000", "Member transaction history retrieved successfully"),
    ADMIN_MEMBER_STATUS_UPDATE_SUCCESS("VYRA-AMM-STS-000", "Member status updated successfully"),
    ADMIN_MEMBER_DELETE_SUCCESS("VYRA-AMM-DEL-000", "Member deleted successfully"),

    // =========================================================================
    // ADMIN REPORTS
    // =========================================================================
    REPORT_INCOME_SUCCESS("VYRA-RPT-INC-000", "Income report retrieved successfully"),
    REPORT_EXPENSE_SUCCESS("VYRA-RPT-EXP-000", "Expense report retrieved successfully"),
    REPORT_TRANSACTION_SUCCESS("VYRA-RPT-TRX-000", "Transaction report retrieved successfully"),
    REPORT_MEMBER_SUCCESS("VYRA-RPT-MBR-000", "Member report retrieved successfully"),

    // =========================================================================
    // ADMIN AUDIT TRAIL
    // =========================================================================
    AUDIT_USER_LIST_SUCCESS("VYRA-AUD-USR-000", "User audit trail retrieved successfully"),
    AUDIT_MEMBER_LIST_SUCCESS("VYRA-AUD-MBR-000", "Member audit trail retrieved successfully");

    private final String code;
    private final String message;

}
