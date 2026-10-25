package com.vyra.be_virtual_your_assets.constant;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class ApiPath {
    // =========================================================================
    // AUTHENTICATION CONTROLLER
    // =========================================================================
    public static final String V1_AUTH = "/api/v1/auth";
    public static final String REGISTER = "/register";
    public static final String RESEND_OTP = "/resend-otp";
    public static final String VERIFY_OTP = "/verify-otp";
    public static final String LOGIN = "/login";
    public static final String REFRESH_TOKEN = "/refresh-token";
    public static final String FORGOT_PIN = "/forgot-pin";
    public static final String RESET_PIN = "/reset-pin";
    public static final String LOGOUT = "/logout";

    // =========================================================================
    // MEMBER CONTROLLER
    // =========================================================================
    public static final String V1_MEMBER = "/api/v1/member";
    public static final String GET_MEMBER = "/get-member";
    public static final String UPDATE_PROFILE = "/update-profile";
    public static final String UPDATE_PROFILE_VERIFY = "/update-profile/verify";
    public static final String UPDATE_PROFILE_RESEND_OTP = "/update-profile/resend-otp";
    public static final String CHANGE_PIN = "/change-pin";
    public static final String GET_MEMBER_ACTIVITY = "/activity";

    // =========================================================================
    // WALLET CONTROLLER
    // =========================================================================
    public static final String V1_WALLET = "/api/v1/wallet";
    public static final String CREATE_WALLET = "/create";
    public static final String GET_WALLET = "/get-wallet/{phoneNumber}";

    // =========================================================================
    // TRANSACTION CONTROLLER
    // =========================================================================
    public static final String V1_TRANSACTION = "/api/v1/transaction";
    public static final String GET_CHART = "/get-chart";
    public static final String CREATE_TRANSACTION = "/create";
    public static final String GET_TOP_TRANSACTION_HISTORY = "/get-top-history";
    public static final String GET_TRANSACTION_HISTORY = "/get-history";

    // =========================================================================
    // TRANSACTIONS (REST, V2): /api/v1/transactions
    // =========================================================================
    public static final String V1_TRANSACTIONS = "/api/v1/transactions";
    public static final String TRANSACTION_ID = "/{transactionId}";

    // =========================================================================
    // FINANCE CATEGORIES: /api/v1/finance/categories
    // =========================================================================
    public static final String V1_FINANCE = "/api/v1/finance";
    public static final String FINANCE_SUMMARY = "/summary";
    public static final String V1_FINANCE_CATEGORIES = "/api/v1/finance/categories";
    public static final String CATEGORY_ID = "/{categoryId}";

    // =========================================================================
    // MOOD: /api/v1/mood-entries, /api/v1/mood-tags
    // =========================================================================
    public static final String V1_MOOD_ENTRIES = "/api/v1/mood-entries";
    public static final String MOOD_TODAY = "/today";
    public static final String MOOD_DATE = "/{localDate}";
    public static final String V1_MOOD_TAGS = "/api/v1/mood-tags";
    public static final String MOOD_TAG_ID = "/{tagId}";

    // =========================================================================
    // HABIT: /api/v1/habits
    // =========================================================================
    public static final String V1_HABITS = "/api/v1/habits";
    public static final String HABIT_TODAY = "/today";
    public static final String HABIT_ID = "/{habitId}";
    public static final String HABIT_ARCHIVE = "/{habitId}/archive";
    public static final String HABIT_UNARCHIVE = "/{habitId}/unarchive";
    public static final String HABIT_COMPLETION = "/{habitId}/completions/{localDate}";

    // =========================================================================
    // HOME: /api/v1/dashboard/summary (member; the portal's is under /admin)
    // =========================================================================
    public static final String V1_HOME_SUMMARY = "/api/v1/dashboard/summary";

    // =========================================================================
    // ACCOUNT: /api/v1/users/me (the old /member endpoints stay for older apps)
    // =========================================================================
    public static final String V1_USERS_ME = "/api/v1/users/me";
    public static final String USERS_ME_PREFERENCES = "/preferences";
    public static final String USERS_ME_PHOTO = "/photo";
    public static final String USERS_ME_DELETE = "/delete";
    public static final String USERS_ME_DELETE_REQUEST = "/delete/request";
    public static final String USERS_ME_DELETE_RESEND = "/delete/resend";

    // =========================================================================
    // GOAL: /api/v1/goals
    // =========================================================================
    public static final String V1_GOALS = "/api/v1/goals";
    public static final String GOAL_ID = "/{goalId}";
    public static final String GOAL_MILESTONES = "/{goalId}/milestones";
    public static final String GOAL_MILESTONE_ID = "/{goalId}/milestones/{milestoneId}";
    public static final String GOAL_PROGRESS = "/{goalId}/progress";

    // =========================================================================
    // PHASE 2: budgets, trends & insights, notifications, push, exports, onboarding
    // =========================================================================
    public static final String V1_BUDGETS = "/api/v1/budgets";
    public static final String BUDGET_ID = "/{budgetId}";
    public static final String BUDGET_COPY_PREVIOUS = "/copy-previous";

    public static final String MOOD_TREND = "/trend";
    public static final String MOOD_TAGS_SUMMARY = "/tags-summary";
    public static final String HABIT_CALENDAR = "/{habitId}/calendar";
    public static final String V1_INSIGHTS_WEEKLY = "/api/v1/insights/weekly";

    public static final String V1_NOTIFICATIONS = "/api/v1/notifications";
    public static final String NOTIFICATION_UNREAD_COUNT = "/unread-count";
    public static final String NOTIFICATION_READ = "/{notificationId}/read";
    public static final String NOTIFICATION_READ_ALL = "/read-all";
    public static final String NOTIFICATION_PREFERENCES = "/preferences";

    public static final String USERS_ME_PUSH_TOKENS = "/push-tokens";
    public static final String USERS_ME_PUSH_TOKEN = "/push-tokens/{token}";

    public static final String TRANSACTIONS_EXPORT = "/export";
    public static final String USERS_ME_EXPORT = "/export";
    public static final String USERS_ME_EXPORT_ID = "/export/{exportId}";
    public static final String USERS_ME_EXPORT_DOWNLOAD = "/export/{exportId}/download";
    /** Public: the e-mailed link, authorised by its token (see SecurityConstant.PUBLIC_URL). */
    public static final String V1_EXPORT_PUBLIC_DOWNLOAD = "/api/v1/exports/{exportId}/download";

    public static final String USERS_ME_ONBOARDING_COMPLETE = "/onboarding/complete";

    // =========================================================================
    // PHASE 3: product configuration, metrics, user reports, insights & AI
    // =========================================================================
    /** Public: maintenance status for the app before / without signing in. */
    public static final String V1_STATUS = "/api/v1/status";
    /** Public: latest published legal document, {type} = privacy | terms. */
    public static final String V1_LEGAL = "/api/v1/legal/{type}";
    public static final String V1_ANNOUNCEMENTS = "/api/v1/announcements";
    public static final String V1_USER_REPORTS = "/api/v1/user-reports";
    public static final String V1_INSIGHTS_ADVANCED = "/api/v1/insights/advanced";
    public static final String V1_REFLECTIONS = "/api/v1/insights/reflections";
    public static final String REFLECTIONS_STATUS = "/status";
    public static final String REFLECTION_ID = "/{reflectionId}";
    public static final String USERS_ME_CONSENTS = "/consents";
    public static final String USERS_ME_CONSENT = "/consents/{type}";

    public static final String V1_ADMIN_CATEGORIES = "/api/v1/admin/categories";
    public static final String ADMIN_CATEGORY_ID = "/{categoryId}";
    public static final String ADMIN_CATEGORY_ACTIVATE = "/{categoryId}/activate";
    public static final String ADMIN_CATEGORY_DEACTIVATE = "/{categoryId}/deactivate";
    public static final String V1_ADMIN_ANNOUNCEMENTS = "/api/v1/admin/announcements";
    public static final String ADMIN_ANNOUNCEMENT_ID = "/{announcementId}";
    public static final String ADMIN_ANNOUNCEMENT_PUBLISH = "/{announcementId}/publish";
    public static final String ADMIN_ANNOUNCEMENT_UNPUBLISH = "/{announcementId}/unpublish";
    public static final String V1_ADMIN_LEGAL = "/api/v1/admin/legal-documents";
    public static final String ADMIN_LEGAL_ID = "/{documentId}";
    public static final String V1_ADMIN_METRICS = "/api/v1/admin/metrics/product";
    public static final String V1_ADMIN_SYSTEM_STATUS = "/api/v1/admin/system/status";
    public static final String V1_ADMIN_USER_REPORTS = "/api/v1/admin/user-reports";
    public static final String ADMIN_USER_REPORT_ID = "/{reportId}";
    public static final String ADMIN_USER_REPORT_ATTACHMENT = "/{reportId}/attachment";

    // =========================================================================
    // ADMIN AUTH CONTROLLER
    // =========================================================================
    public static final String V1_ADMIN_AUTH = "/api/v1/admin/auth";
    public static final String ADMIN_LOGIN = "/login";
    public static final String ADMIN_REFRESH_TOKEN = "/refresh-token";
    public static final String ADMIN_LOGOUT = "/logout";

    // =========================================================================
    // ADMIN CONTROLLER (admin user / RBAC management)
    // =========================================================================
    public static final String V1_ADMIN = "/api/v1/admin";
    public static final String ADMIN_LIST = "/list";
    public static final String ADMIN_REGISTER = "/register";
    public static final String ADMIN_DETAIL = "/{adminId}";
    public static final String ADMIN_UPDATE = "/{adminId}";
    public static final String ADMIN_DELETE = "/{adminId}";
    public static final String ADMIN_UPDATE_STATUS = "/{adminId}/status";
    public static final String ADMIN_RESET_PASSWORD = "/{adminId}/reset-password";
    public static final String ADMIN_PROFILE = "/profile";
    public static final String ADMIN_ME = "/me";

    // =========================================================================
    // ADMIN DASHBOARD CONTROLLER
    // =========================================================================
    public static final String V1_ADMIN_DASHBOARD = "/api/v1/admin/dashboard";
    public static final String DASHBOARD_SUMMARY = "/summary";
    public static final String DASHBOARD_FINANCIAL_CHART = "/financial-chart";
    public static final String DASHBOARD_CATEGORY_DISTRIBUTION = "/category-distribution";
    public static final String DASHBOARD_RECENT_TRANSACTIONS = "/recent-transactions";
    public static final String DASHBOARD_LATEST_MEMBERS = "/latest-members";

    // =========================================================================
    // ADMIN MEMBER MANAGEMENT CONTROLLER
    // =========================================================================
    public static final String V1_ADMIN_MEMBER = "/api/v1/admin/members";
    public static final String ADMIN_MEMBER_LIST = "";
    public static final String ADMIN_MEMBER_DETAIL = "/{memberId}";
    public static final String ADMIN_MEMBER_TRANSACTIONS = "/{memberId}/transactions";
    public static final String ADMIN_MEMBER_STATUS = "/{memberId}/status";
    public static final String ADMIN_MEMBER_DELETE = "/{memberId}";
    public static final String ADMIN_MEMBER_EXPORT = "/export";

    // =========================================================================
    // ADMIN REPORT CONTROLLER
    // =========================================================================
    public static final String V1_ADMIN_REPORT = "/api/v1/admin/reports";
    public static final String REPORT_INCOME = "/income";
    public static final String REPORT_EXPENSE = "/expense";
    public static final String REPORT_TRANSACTION = "/transaction";
    public static final String REPORT_MEMBER = "/member";
    public static final String REPORT_EXPORT_CSV = "/export";

    // =========================================================================
    // ADMIN SETTINGS CONTROLLER
    // =========================================================================
    public static final String V1_ADMIN_SETTINGS = "/api/v1/admin/settings";
    public static final String SETTINGS_LIST = "";
    public static final String SETTINGS_UPDATE = "/{configKey}";

    // =========================================================================
    // ADMIN AUDIT CONTROLLER
    // =========================================================================
    public static final String V1_ADMIN_AUDIT = "/api/v1/admin/audit";
    public static final String AUDIT_USER = "/user";
    public static final String AUDIT_MEMBER = "/member";

}
