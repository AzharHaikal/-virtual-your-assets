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
    // ADMIN AUDIT CONTROLLER
    // =========================================================================
    public static final String V1_ADMIN_AUDIT = "/api/v1/admin/audit";
    public static final String AUDIT_USER = "/user";
    public static final String AUDIT_MEMBER = "/member";

}
