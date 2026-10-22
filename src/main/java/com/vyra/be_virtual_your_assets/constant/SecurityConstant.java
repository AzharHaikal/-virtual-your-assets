package com.vyra.be_virtual_your_assets.constant;
public class SecurityConstant {

    private SecurityConstant() {}

    public static final String[] WHITE_LIST_URL = {
            "/api/v1/auth/register",
            "/api/v1/auth/resend-otp",
            "/api/v1/auth/verify-otp",
            "/api/v1/auth/login",
            "/api/v1/auth/refresh-token",
            "/api/v1/auth/forgot-pin",
            "/api/v1/auth/reset-pin",
    };

    public static final String[] ADMIN_WHITE_LIST_URL = {
            "/api/v1/admin/auth/login",
            "/api/v1/admin/auth/refresh-token",
    };

    public static final String ADMIN_PATH_PREFIX = "/api/v1/admin/**";

    /** OpenAPI spec and Swagger UI; only served when springdoc is enabled (404 otherwise). */
    public static final String[] API_DOCS_URL = {
            "/v3/api-docs",
            "/v3/api-docs/**",
            "/swagger-ui.html",
            "/swagger-ui/**",
    };
}
