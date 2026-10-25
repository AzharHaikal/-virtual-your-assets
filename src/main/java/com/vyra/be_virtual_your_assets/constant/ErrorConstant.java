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
    INVALID_DATE_RANGE("VYRA-INV-005", "The start date must be on or before the end date"),
    INVALID_SORT("VYRA-INV-006", "Sort must be date or amount, optionally followed by ,asc or ,desc"),

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
    TRANSACTION_LIST_SUCCESS("VYRA-TRX-LST-000", "Transactions retrieved successfully"),
    FINANCE_SUMMARY_SUCCESS("VYRA-FIN-SUM-000", "Finance summary retrieved successfully"),
    TRANSACTION_NOT_EDITABLE("VYRA-TRX-001", "Only successful transactions can be changed"),

    // Finance categories
    CATEGORY_LIST_SUCCESS("VYRA-CAT-LST-000", "Categories retrieved successfully"),
    CATEGORY_CREATE_SUCCESS("VYRA-CAT-CRT-000", "Category created successfully"),
    CATEGORY_UPDATE_SUCCESS("VYRA-CAT-UPD-000", "Category updated successfully"),
    CATEGORY_DELETE_SUCCESS("VYRA-CAT-DEL-000", "Category deleted successfully"),
    CATEGORY_NOT_FOUND("VYRA-CAT-404", "Category could not be found"),
    CATEGORY_TYPE_MISMATCH("VYRA-CAT-001", "The category does not match the transaction type"),
    CATEGORY_NAME_EXISTS("VYRA-CAT-002", "You already have a category with this name"),
    CATEGORY_LIMIT_REACHED("VYRA-CAT-003", "You can create up to 50 categories"),

    // Mood
    MOOD_SAVE_SUCCESS("VYRA-MOD-SAV-000", "Mood saved"),
    MOOD_LIST_SUCCESS("VYRA-MOD-LST-000", "Mood entries retrieved successfully"),
    MOOD_GET_SUCCESS("VYRA-MOD-GET-000", "Mood entry retrieved successfully"),
    MOOD_DELETE_SUCCESS("VYRA-MOD-DEL-000", "Mood entry deleted"),
    MOOD_NOT_FOUND("VYRA-MOD-404", "No mood entry for this day"),
    MOOD_DATE_IN_FUTURE("VYRA-MOD-001", "You cannot check in for a future day"),
    MOOD_TAG_LIST_SUCCESS("VYRA-MTG-LST-000", "Mood tags retrieved successfully"),
    MOOD_TAG_CREATE_SUCCESS("VYRA-MTG-CRT-000", "Mood tag created"),
    MOOD_TAG_DELETE_SUCCESS("VYRA-MTG-DEL-000", "Mood tag deleted"),
    MOOD_TAG_NOT_FOUND("VYRA-MTG-404", "Mood tag could not be found"),
    MOOD_TAG_NAME_EXISTS("VYRA-MTG-002", "A tag with this name already exists"),
    MOOD_TAG_LIMIT_REACHED("VYRA-MTG-003", "You can create up to 30 tags"),
    HABIT_LIST_SUCCESS("VYRA-HBT-LST-000", "Habits retrieved successfully"),
    HABIT_TODAY_SUCCESS("VYRA-HBT-TDY-000", "Today's habits retrieved successfully"),
    HABIT_CREATE_SUCCESS("VYRA-HBT-CRT-000", "Habit created"),
    HABIT_UPDATE_SUCCESS("VYRA-HBT-UPD-000", "Habit updated"),
    HABIT_DELETE_SUCCESS("VYRA-HBT-DEL-000", "Habit deleted"),
    HABIT_ARCHIVE_SUCCESS("VYRA-HBT-ARC-000", "Habit archived"),
    HABIT_UNARCHIVE_SUCCESS("VYRA-HBT-UAR-000", "Habit restored"),
    HABIT_COMPLETION_SUCCESS("VYRA-HBT-CMP-000", "Progress saved"),
    HABIT_COMPLETION_DELETE_SUCCESS("VYRA-HBT-UCM-000", "Progress removed"),
    HABIT_NOT_FOUND("VYRA-HBT-404", "Habit could not be found"),
    HABIT_DATE_IN_FUTURE("VYRA-HBT-001", "You cannot mark a future day"),
    HABIT_DATE_TOO_OLD("VYRA-HBT-002", "You can only mark the last 7 days"),
    HABIT_NOT_SCHEDULED("VYRA-HBT-003", "This habit is not scheduled on that day"),
    HABIT_ARCHIVED("VYRA-HBT-004", "This habit is archived"),
    HABIT_SCHEDULE_DAYS_REQUIRED("VYRA-HBT-005", "Choose at least one day"),
    HABIT_LIMIT_REACHED("VYRA-HBT-006", "You can have up to 50 habits"),
    HOME_SUMMARY_SUCCESS("VYRA-HOM-000", "Home summary retrieved successfully"),
    ACCOUNT_GET_SUCCESS("VYRA-ACC-000", "Account retrieved successfully"),
    ACCOUNT_PREFERENCES_SUCCESS("VYRA-ACC-PRF-000", "Preferences saved"),
    ACCOUNT_PHOTO_SUCCESS("VYRA-ACC-PHO-000", "Profile photo saved"),
    ACCOUNT_PHOTO_DELETE_SUCCESS("VYRA-ACC-PHD-000", "Profile photo removed"),
    ACCOUNT_PHOTO_NOT_FOUND("VYRA-ACC-404", "No profile photo yet"),
    ACCOUNT_CURRENCY_UNSUPPORTED("VYRA-ACC-001", "This currency is not supported"),
    ACCOUNT_PHOTO_INVALID("VYRA-ACC-002", "Use a JPEG, PNG or WebP image"),
    ACCOUNT_PHOTO_TOO_LARGE("VYRA-ACC-003", "The photo must be 1 MB or smaller"),
    ACCOUNT_DELETE_OTP_SENT("VYRA-ACC-DEL-001", "A verification code has been sent to your e-mail. Enter it to delete your account"),
    ACCOUNT_DELETE_SUCCESS("VYRA-ACC-DEL-000", "Your account has been deleted"),
    GOAL_LIST_SUCCESS("VYRA-GOL-LST-000", "Goals retrieved successfully"),
    GOAL_DETAIL_SUCCESS("VYRA-GOL-DTL-000", "Goal retrieved successfully"),
    GOAL_CREATE_SUCCESS("VYRA-GOL-CRT-000", "Goal created"),
    GOAL_UPDATE_SUCCESS("VYRA-GOL-UPD-000", "Goal updated"),
    GOAL_DELETE_SUCCESS("VYRA-GOL-DEL-000", "Goal deleted"),
    GOAL_PROGRESS_SUCCESS("VYRA-GOL-PRG-000", "Progress saved"),
    GOAL_MILESTONE_CREATE_SUCCESS("VYRA-GML-CRT-000", "Milestone added"),
    GOAL_MILESTONE_UPDATE_SUCCESS("VYRA-GML-UPD-000", "Milestone updated"),
    GOAL_MILESTONE_DELETE_SUCCESS("VYRA-GML-DEL-000", "Milestone deleted"),
    GOAL_NOT_FOUND("VYRA-GOL-404", "Goal could not be found"),
    GOAL_IMAGE_SUCCESS("VYRA-GOL-IMG-000", "Goal image saved"),
    GOAL_IMAGE_DELETE_SUCCESS("VYRA-GOL-IMD-000", "Goal image removed"),
    GOAL_IMAGE_NOT_FOUND("VYRA-GOL-IMG-404", "This goal has no image"),
    GOAL_IMAGE_INVALID("VYRA-GOL-IMG-001", "Use a JPEG, PNG or WebP image"),
    GOAL_IMAGE_TOO_LARGE("VYRA-GOL-IMG-002", "The image must be 1 MB or smaller"),
    GOAL_MILESTONE_NOT_FOUND("VYRA-GML-404", "Milestone could not be found"),
    GOAL_TARGET_REQUIRED("VYRA-GOL-001", "Enter a target for this goal"),
    GOAL_VALUE_NOT_ALLOWED("VYRA-GOL-002", "This goal is measured by its milestones, not by a value"),
    GOAL_DUE_DATE_IN_PAST("VYRA-GOL-003", "The due date cannot be in the past"),
    GOAL_VALUE_NEGATIVE("VYRA-GOL-004", "The progress cannot go below 0"),
    GOAL_PROGRESS_EMPTY("VYRA-GOL-005", "Enter a value or a note"),
    GOAL_LIMIT_REACHED("VYRA-GOL-006", "You can have up to 50 goals"),
    GOAL_MILESTONE_LIMIT_REACHED("VYRA-GML-001", "A goal can have up to 50 milestones"),
    GOAL_FINANCE_LINK_INVALID("VYRA-GOL-007", "Only a goal measured by a value in IDR can be linked to your finances"),

    // Budgets (Phase 2.1)
    BUDGET_LIST_SUCCESS("VYRA-BDG-LST-000", "Budgets retrieved successfully"),
    BUDGET_CREATE_SUCCESS("VYRA-BDG-CRT-000", "Budget created"),
    BUDGET_UPDATE_SUCCESS("VYRA-BDG-UPD-000", "Budget updated"),
    BUDGET_DELETE_SUCCESS("VYRA-BDG-DEL-000", "Budget deleted"),
    BUDGET_COPY_SUCCESS("VYRA-BDG-CPY-000", "Budgets copied from the previous month"),
    BUDGET_NOT_FOUND("VYRA-BDG-404", "Budget could not be found"),
    BUDGET_CATEGORY_NOT_EXPENSE("VYRA-BDG-001", "A budget can only be set for an expense category"),
    BUDGET_ALREADY_EXISTS("VYRA-BDG-002", "This category already has a budget for that month"),

    // Trends & insights (Phase 2.3)
    MOOD_TREND_SUCCESS("VYRA-MOD-TRD-000", "Mood trend retrieved successfully"),
    MOOD_TAGS_SUMMARY_SUCCESS("VYRA-MOD-TAG-000", "Mood tag summary retrieved successfully"),
    HABIT_CALENDAR_SUCCESS("VYRA-HBT-CAL-000", "Habit calendar retrieved successfully"),
    HABIT_REMINDER_TIME_REQUIRED("VYRA-HBT-007", "Choose a time for the reminder"),
    INSIGHT_WEEKLY_SUCCESS("VYRA-INS-WKL-000", "Weekly summary retrieved successfully"),
    INVALID_TREND_PERIOD("VYRA-INV-006", "Invalid period. Use week or month"),

    // Notifications & push (Phase 2.4 - 2.6)
    NOTIFICATION_LIST_SUCCESS("VYRA-NTF-LST-000", "Notifications retrieved successfully"),
    NOTIFICATION_UNREAD_COUNT_SUCCESS("VYRA-NTF-CNT-000", "Unread notifications counted"),
    NOTIFICATION_READ_SUCCESS("VYRA-NTF-RD-000", "Notification marked as read"),
    NOTIFICATION_READ_ALL_SUCCESS("VYRA-NTF-RDA-000", "All notifications marked as read"),
    NOTIFICATION_NOT_FOUND("VYRA-NTF-404", "Notification could not be found"),
    NOTIFICATION_PREFERENCES_SUCCESS("VYRA-NTF-PRF-000", "Notification settings retrieved successfully"),
    NOTIFICATION_PREFERENCES_SAVED("VYRA-NTF-PRF-001", "Notification settings saved"),
    NOTIFICATION_QUIET_HOURS_INVALID("VYRA-NTF-001", "Set both the start and the end of quiet hours, and make them different"),
    PUSH_TOKEN_SAVED("VYRA-PSH-000", "Device registered for notifications"),
    PUSH_TOKEN_DELETED("VYRA-PSH-DEL-000", "Device removed from notifications"),

    // Exports (Phase 2.7)
    EXPORT_RANGE_TOO_LONG("VYRA-EXP-001", "Export at most 366 days at a time"),
    DATA_EXPORT_REQUESTED("VYRA-EXP-REQ-000", "We are preparing your data. You will get an e-mail when it is ready"),
    DATA_EXPORT_GET_SUCCESS("VYRA-EXP-GET-000", "Export status retrieved successfully"),
    DATA_EXPORT_NOT_FOUND("VYRA-EXP-404", "Export could not be found"),
    DATA_EXPORT_NOT_READY("VYRA-EXP-002", "Your export is not ready yet"),
    DATA_EXPORT_EXPIRED("VYRA-EXP-410", "This download link has expired. Please request a new export"),
    DATA_EXPORT_TOO_SOON("VYRA-EXP-429", "You can request one export per day. Please try again in"),

    // Product configuration (Phase 3.2)
    SYSTEM_STATUS_SUCCESS("VYRA-STS-000", "Status retrieved successfully"),
    MAINTENANCE_MODE("VYRA-MNT-503", "VYRA is under maintenance. Please try again later"),
    OTP_CHANNEL_REQUIRED("VYRA-SET-003", "At least one OTP channel must stay on, and WhatsApp needs a configured provider"),
    ADMIN_CATEGORY_LIST_SUCCESS("VYRA-ACT-LST-000", "Built-in categories retrieved successfully"),
    ADMIN_CATEGORY_SAVE_SUCCESS("VYRA-ACT-SAV-000", "Built-in category saved"),
    ADMIN_CATEGORY_PROTECTED("VYRA-ACT-001", "The \"Other\" categories are needed as a fallback and cannot be deactivated"),
    ANNOUNCEMENT_LIST_SUCCESS("VYRA-ANN-LST-000", "Announcements retrieved successfully"),
    ANNOUNCEMENT_SAVE_SUCCESS("VYRA-ANN-SAV-000", "Announcement saved"),
    ANNOUNCEMENT_DELETE_SUCCESS("VYRA-ANN-DEL-000", "Announcement deleted"),
    ANNOUNCEMENT_NOT_FOUND("VYRA-ANN-404", "Announcement could not be found"),
    ANNOUNCEMENT_PERIOD_INVALID("VYRA-ANN-001", "The end must be after the start"),
    LEGAL_DOCUMENT_SUCCESS("VYRA-LGL-000", "Legal document retrieved successfully"),
    LEGAL_DOCUMENT_PUBLISHED("VYRA-LGL-PUB-000", "Legal document published"),
    LEGAL_DOCUMENT_NOT_FOUND("VYRA-LGL-404", "No published version of this document yet"),

    // Product metrics (Phase 3.3)
    PRODUCT_METRICS_SUCCESS("VYRA-MTR-000", "Product metrics retrieved successfully"),

    // User reports (Phase 3.4)
    USER_REPORT_CREATED("VYRA-URP-CRT-000", "Thank you, your report was sent"),
    USER_REPORT_LIST_SUCCESS("VYRA-URP-LST-000", "Reports retrieved successfully"),
    USER_REPORT_DETAIL_SUCCESS("VYRA-URP-DTL-000", "Report retrieved successfully"),
    USER_REPORT_UPDATE_SUCCESS("VYRA-URP-UPD-000", "Report updated"),
    USER_REPORT_NOT_FOUND("VYRA-URP-404", "Report could not be found"),
    USER_REPORT_ATTACHMENT_INVALID("VYRA-URP-001", "Attach a JPEG, PNG or WebP image of at most 1 MB"),
    USER_REPORT_TOO_MANY("VYRA-URP-429", "You have sent many reports today. Please try again in"),
    USER_REPORT_ASSIGNEE_INVALID("VYRA-URP-002", "The report can only be assigned to an active admin who can handle reports"),

    // Advanced analytics & AI reflection (Phase 3.5)
    INSIGHT_ADVANCED_SUCCESS("VYRA-INS-ADV-000", "Insights retrieved successfully"),
    CONSENT_LIST_SUCCESS("VYRA-CNS-000", "Consents retrieved successfully"),
    CONSENT_SAVED("VYRA-CNS-001", "Consent saved"),
    CONSENT_TYPE_NOT_FOUND("VYRA-CNS-404", "Unknown consent type"),
    AI_REFLECTION_LIST_SUCCESS("VYRA-AIR-LST-000", "Reflections retrieved successfully"),
    AI_REFLECTION_STATUS_SUCCESS("VYRA-AIR-STS-000", "Reflection status retrieved successfully"),
    AI_REFLECTION_CREATED("VYRA-AIR-CRT-000", "Reflection created"),
    AI_REFLECTION_DELETED("VYRA-AIR-DEL-000", "Reflection deleted"),
    AI_REFLECTION_NOT_FOUND("VYRA-AIR-404", "Reflection could not be found"),
    AI_REFLECTION_CONSENT_REQUIRED("VYRA-AIR-001", "Turn on AI reflection in your privacy settings first"),
    AI_REFLECTION_UNAVAILABLE("VYRA-AIR-002", "AI reflection is not available right now"),
    AI_REFLECTION_EXISTS("VYRA-AIR-003", "There is already a reflection for this week. Delete it to create a new one"),
    AI_REFLECTION_NO_DATA("VYRA-AIR-004", "Not enough data for that week yet"),

    // Onboarding (Phase 2.8)
    ONBOARDING_COMPLETE_SUCCESS("VYRA-ACC-ONB-000", "Welcome aboard"),
    ACCOUNT_MODULES_REQUIRED("VYRA-ACC-004", "Choose at least one module"),

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
