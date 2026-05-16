package com.vyra.virtual_your_assets.constant;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class ApiPath {
    // Authentication Controller
    public static final String V1_AUTH = "/api/v1/auth";
    public static final String REGISTER = "/register";
    public static final String RESEND_OTP = "/resend-otp";
    public static final String VERIFY_OTP = "/verify-otp";
    public static final String LOGIN = "/login";
    public static final String REFRESH_TOKEN = "/refresh-token";
    public static final String FORGOT_PIN = "/forgot-pin";
    public static final String RESET_PIN = "/reset-pin";
    public static final String LOGOUT = "/logout";

    // Member Controller
    public static final String V1_MEMBER = "/api/v1/member";
    public static final String GET_MEMBER = "/get-member";
    // public static final String GET_MEMBER_BY_PHONE_NUMBER = "/get-member/{phoneNumber}";
    // public static final String GET_MEMBER_BY_MEMBER_ID = "/get-member/{phoneNumber}";
    public static final String EDIT_PROFILE = "/edit-profile";

    // Wallet Controller
    public static final String V1_WALLET = "/api/v1/wallet";
    public static final String CREATE_WALLET = "/create";
    public static final String GET_WALLET = "/get-wallet/{phoneNumber}";

    // Transaction Controller
    public static final String V1_TRANSACTION = "/api/v1/transaction";
    public static final String CREATE_TRANSACTION = "/create";
    public static final String GET_HISTORY_TRANSACTION = "/history";


}
