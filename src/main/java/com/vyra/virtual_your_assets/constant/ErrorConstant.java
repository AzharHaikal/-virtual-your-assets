package com.vyra.virtual_your_assets.constant;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum ErrorConstant {

    REGISTER_SUCCESS("VYRA-REG-000", "Registration completed successfully"),

    LOGIN_SUCCESS("VYRA-LGN-000", "Login successful"),

    FORGOT_PASSWORD_OTP_SENT("VYRA-FP-001", "Success generate, an OTP has been sent"),
    RESET_PIN_SUCCESS("VYRA-FP-002", "PIN has been reset successfully"),

    TOKEN_CREATED("VYRA-TKN-001", "Session token created successfully"),
    TOKEN_EXPIRED("VYRA-TKN-002", "Session token is no longer valid"),

    // Invalid
    INVALID_PIN("VYRA-INV-001", "Invalid PIN. Please try again"),

    // Not Found (generic to prevent enumeration)
    PHONE_NUMBER_NOT_FOUND("VYRA-DNF-001", "Invalid account. Please check your account details"),
    EMAIL_NOT_FOUND("VYRA-DNF-002", "Invalid account. Please check your account details"),
    MEMBER_NOT_FOUND("VYRA-DNF-003", "Invalid account. Please check your account details"),
    MEMBER_NOT_ACTIVE("VYRA-DNF-004", "Invalid account. Please check your account status"),

    // Exists (generic)
    PHONE_NUMBER_ALREADY_EXIST("VYRA-EXS-001", "Account already exists"),
    EMAIL_ALREADY_EXIST("VYRA-EXS-002", "Account already exists"),

    // OTP
    VERIFY_OTP_SUCCESS("VYRA-OTP-000", "OTP verified successfully"),
    RESEND_OTP("VYRA-OTP-001", "If the account exists, an OTP has been sent"),
    OTP_NOT_FOUND("VYRA-OTP-002", "Invalid or expired OTP"),
    OTP_EXPIRED("VYRA-OTP-003", "Invalid or expired OTP"),
    OTP_INVALID("VYRA-OTP-004", "Invalid or expired OTP"),
    MAX_ATTEMPTS_REACHED("VYRA-OTP-005", "Maximum verification attempts reached. Your registration data has been cleared. Please register again."),

    // Exception
    BAD_REQUEST("VYRA-ERR400", "Invalid request"),
    DATA_NOT_FOUND("VYRA-ERR404", "Requested data could not be found"),
    INTERNAL_SERVER_ERROR("VYRA-ERR500", "An unexpected error occurred");

    private final String code;
    private final String message;

}
