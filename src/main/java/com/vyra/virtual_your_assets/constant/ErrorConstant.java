package com.vyra.virtual_your_assets.constant;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum ErrorConstant {

    REGISTER_SUCCESS("VYRA-REG-000", "Your registration was successful"),
    REGISTER_FAILED("VYRA-REG-001", "Your registration was failed"),

    CREATE_WALLET_SUCCESS("VYRA-WLT-000", "Create wallet was successful"),
    CREATE_WALLET_FAILED("VYRA-WLT-001", "Create wallet was failed"),

    EMAIL_SEND_FAILED("VYRA-EML-001", "Failed when sent otp via email"),

    LOGIN_SUCCESS("VYRA-LGN-000", "You've successfully logged in"),

    FORGOT_PASSWORD_OTP_SENT("VYRA-FP-001", "An OTP has been sent"),
    RESET_PIN_SUCCESS("VYRA-FP-002", "Your PIN has been successfully updated"),

    TOKEN_CREATED("VYRA-TKN-001", "Your session has been created"),
    TOKEN_EXPIRED("VYRA-TKN-002", "Your session has expired. Please log in again"),

    // Invalid
    INVALID_PIN("VYRA-INV-001", "Invalid PIN. Please try again"),

    // Not Found (generic to prevent enumeration)
    PHONE_NUMBER_NOT_FOUND("VYRA-DNF-001", "Invalid account. Please check your account details"),
    EMAIL_NOT_FOUND("VYRA-DNF-002", "Invalid account. Please check your account details"),
    MEMBER_NOT_FOUND("VYRA-DNF-003", "Invalid account. Please check your account details"),
    MEMBER_NOT_ACTIVE("VYRA-DNF-004", "Invalid account. Please check your account status"),

    // Exists (generic)
    PHONE_NUMBER_ALREADY_EXIST("VYRA-EXS-001", "Your account already exists. Please use a different account"),
    EMAIL_ALREADY_EXIST("VYRA-EXS-002", "Your account already exists. Please use a different account"),

    // OTP
    VERIFY_OTP_SUCCESS("VYRA-OTP-000", "OTP verification successful"),
    RESEND_OTP("VYRA-OTP-001", "If the account exists, a new OTP has been sent"),
    OTP_NOT_FOUND("VYRA-OTP-002", "Invalid or expired OTP"),
    OTP_EXPIRED("VYRA-OTP-003", "The OTP has expired. Please request a new one"),
    OTP_INVALID("VYRA-OTP-004", "Invalid OTP. Please enter the OTP correctly. Remaining attempts - "),
    MAX_ATTEMPTS_REACHED("VYRA-OTP-005", "You've reached the maximum attempts. Please register again"),

    // Exception
    BAD_REQUEST("VYRA-ERR400", "Something went wrong with your request"),
    DATA_NOT_FOUND("VYRA-ERR404", "The requested data could not be found"),
    INTERNAL_SERVER_ERROR("VYRA-ERR500", "We are experiencing a system issue. Please try again later");

    private final String code;
    private final String message;

}
