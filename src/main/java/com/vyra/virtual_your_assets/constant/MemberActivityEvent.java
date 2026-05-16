package com.vyra.virtual_your_assets.constant;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum MemberActivityEvent {

    // REGISTER
    ATTEMPT_REGISTER("Member attempted to register an account"),
    SUCCESS_REGISTER("Member successfully registered an account"),
    FAILED_REGISTER("Member failed to register"),
    ATTEMPT_GENERATE_OTP_REGISTER("Member requested OTP for register"),
    SUCCESS_GENERATE_OTP_REGISTER("OTP for register was successfully generated"),

    // Wallet
    ATTEMPT_CREATE_WALLET("Member attempted to create wallet"),
    SUCCESS_CREATE_WALLET("Member successfully create wallet"),

    // LOGIN
    ATTEMPT_LOGIN("Member attempted to log in"),
    SUCCESS_LOGIN("Member successfully logged in"),

    ATTEMPT_LOGOUT("Member attempted to log out"),
    SUCCESS_LOGOUT("Member successfully logged out"),

    // RESEND OTP
    ATTEMPT_RESEND_OTP("Member requested to resend OTP"),
    SUCCESS_RESEND_OTP("OTP was successfully sent to the user"),

    // VERIFY OTP
    ATTEMPT_VERIFY_OTP("Member attempted to verify OTP"),
    SUCCESS_VERIFY_OTP("Member was successfully verified OTP"),

    // FORGOT PASSWORD
    ATTEMPT_GENERATE_FORGOT_PASSWORD("Member requested OTP for password reset"),
    SUCCESS_GENERATE_FORGOT_PASSWORD("OTP for password reset was successfully generated"),

    // RESET PASSWORD
    ATTEMPT_RESET_PASSWORD("Member attempted to reset the password"),
    SUCCESS_RESET_PASSWORD("Member successfully reset the password"),

    // GET MEMBER
    ATTEMPT_GET_MEMBER_DETAIL("Attempt get member detail"),
    SUCCESS_GET_MEMBER_DETAIL("Successfully get member detail"),
    FAILED_GET_MEMBER_DETAIL("Failed get member detail"),

    // UPDATE PROFILE
    ATTEMPT_UPDATE_PROFILE("Attempt update member profile"),
    SUCCESS_UPDATE_PROFILE("Successfully updated member profile"),

    // GET MEMBER WALLET
    ATTEMPT_GET_MEMBER_WALLET("Attempt get member wallet"),
    SUCCESS_GET_MEMBER_WALLET("Successfully get member wallet"),

    // TRANSACTION
    ATTEMPT_CREATE_TRANSACTION("Attempt create transaction"),
    SUCCESS_CREATE_TRANSACTION("Successfully create transaction"),
    FAILED_CREATE_TRANSACTION("Failed create transaction"),

    ATTEMPT_GET_TRANSACTION_HISTORY("Attempt get transaction history"),
    SUCCESS_GET_TRANSACTION_HISTORY("Successfully get transaction history"),

    // TRANSACTION WALLET
    ATTEMPT_INSERT_WALLET_STATEMENT("Attempt insert transaction wallet statement"),
    SUCCESS_INSERT_WALLET_STATEMENT("Attempt insert transaction wallet statement");

    private final String description;


}
