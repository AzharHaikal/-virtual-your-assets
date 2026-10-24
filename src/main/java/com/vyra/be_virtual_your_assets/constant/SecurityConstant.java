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

    /**
     * Reachable without a member token: the health check for the uptime monitor and the download link
     * of a data export (opened from the e-mail, protected by its own one-time token).
     */
    public static final String[] PUBLIC_URL = {
            "/actuator/health",
            "/actuator/health/**",
            "/api/v1/exports/*/download",
    };

    /** Prometheus metrics: only with the header {@link #METRICS_TOKEN_HEADER} = {@code vyra.metrics.token}. */
    public static final String METRICS_URL = "/actuator/prometheus";
    public static final String METRICS_TOKEN_HEADER = "X-Metrics-Token";

    /** OpenAPI spec and Swagger UI; only served when springdoc is enabled (404 otherwise). */
    public static final String[] API_DOCS_URL = {
            "/v3/api-docs",
            "/v3/api-docs/**",
            "/swagger-ui.html",
            "/swagger-ui/**",
    };
}
