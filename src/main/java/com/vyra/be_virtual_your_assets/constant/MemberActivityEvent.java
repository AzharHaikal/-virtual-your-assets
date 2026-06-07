package com.vyra.be_virtual_your_assets.constant;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum MemberActivityEvent {

    // REGISTER
    ATTEMPT_REGISTER("Member attempted to register an account"),
    SUCCESS_REGISTER("Member successfully registered an account"),
    FAILED_REGISTER("Member failed to register an account"),

    // OTP
    ATTEMPT_GENERATE_OTP_REGISTER("Member requested OTP for registration"),
    SUCCESS_GENERATE_OTP_REGISTER("OTP for registration was successfully generated"),
    FAILED_GENERATE_OTP_REGISTER("Failed to generate or send OTP"),

    // WALLET
    ATTEMPT_CREATE_WALLET("Member attempted to create a wallet"),
    SUCCESS_CREATE_WALLET("Member successfully created a wallet"),

    // LOGIN & LOGOUT
    ATTEMPT_LOGIN("Member attempted to log in"),
    SUCCESS_LOGIN("Member successfully logged in"),
    FAILED_LOGIN("Member failed to log in"),
    ATTEMPT_LOGOUT("Member attempted to log out"),
    SUCCESS_LOGOUT("Member successfully logged out"),

    // RESEND OTP
    ATTEMPT_RESEND_OTP("Member requested to resend OTP"),
    SUCCESS_RESEND_OTP("OTP was successfully resent to the user"),

    // VERIFY OTP
    ATTEMPT_VERIFY_OTP("Member attempted to verify OTP"),
    SUCCESS_VERIFY_OTP("Member successfully verified the OTP"),

    // FORGOT PIN & RESET PIN
    ATTEMPT_GENERATE_FORGOT_PIN("Member requested OTP for PIN reset"),
    SUCCESS_GENERATE_FORGOT_PIN("OTP for PIN reset was successfully generated"),
    ATTEMPT_RESET_PIN("Member attempted to reset the PIN"),
    SUCCESS_RESET_PIN("Member successfully reset the PIN"),

    // CHANGE PIN
    ATTEMPT_CHANGE_PIN("Member requested to change PIN"),
    SUCCESS_CHANGE_PIN("Member successfully changed the PIN"),
    FAILED_CHANGE_PIN("Member failed to changed the PIN"),

    // UPDATE PROFILE
    ATTEMPT_UPDATE_PROFILE("Member attempted to update profile"),
    SUCCESS_UPDATE_PROFILE("Member successfully updated the profile"),

    // TRANSACTION
    ATTEMPT_CREATE_TRANSACTION("Member attempted to create a transaction"),
    SUCCESS_CREATE_TRANSACTION("Member successfully created the transaction"),
    FAILED_CREATE_TRANSACTION("Member failed to create a transaction"),

    // TRANSACTION WALLET / STATEMENT (Sistem internal ledger)
    ATTEMPT_INSERT_WALLET_STATEMENT("Attempted to update the wallet statement"),
    SUCCESS_INSERT_WALLET_STATEMENT("Successfully updated the wallet statement");

    /* OPTIONAL: READ-ONLY LOGS
    ATTEMPT_GET_MEMBER_DETAIL("Member attempted to fetch profile details"),
    SUCCESS_GET_MEMBER_DETAIL("Member successfully fetched profile details"),
    FAILED_GET_MEMBER_DETAIL("Member failed to fetch profile details"),

    ATTEMPT_GET_MEMBER_WALLET("Member attempted to fetch wallet details"),
    SUCCESS_GET_MEMBER_WALLET("Member successfully fetched wallet details"),

    ATTEMPT_GET_TRANSACTION_HISTORY("Member attempted to fetch transaction history"),
    SUCCESS_GET_TRANSACTION_HISTORY("Member successfully fetched transaction history");
    */
    private final String description;

}
