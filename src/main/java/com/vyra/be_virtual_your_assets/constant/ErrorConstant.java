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
    MAX_ATTEMPTS_REACHED("VYRA-OTP-005", "You've reached the maximum attempts. Your account has been suspended"),

    // Member
    GET_MEMBER_SUCCESS("VYRA-GMS-000", "Get member detail successful"),
    GET_MEMBER_FAILED("VYRA-GMS-001", "Get member detail failed"),

    // Update member profile
    UPDATE_PROFILE_SUCCESS("VYRA-UPS-000", "Update member profile successful"),
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

    // Transaction History
    GET_TOP_TRANSACTION_HISTORY_SUCCESS("VYRA-GTT-000","Top transaction history retrieved successfully"),
    GET_TRANSACTION_HISTORY_SUCCESS("VYRA-GTH-000","Transaction history retrieved successfully"),

    // Member Change Pin
    CHANGE_PIN_SUCCESS("VYRA-CPS-000", "Change pin successful"),

    // Exception
    BAD_REQUEST("VYRA-ERR400", "Something went wrong with your request"),
    DATA_NOT_FOUND("VYRA-ERR404", "The requested data could not be found"),
    INTERNAL_SERVER_ERROR("VYRA-ERR500", "We are experiencing a system issue. Please try again later");

    private final String code;
    private final String message;

}
