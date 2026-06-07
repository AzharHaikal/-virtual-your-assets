package com.vyra.virtual_your_assets.constant;
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
}
