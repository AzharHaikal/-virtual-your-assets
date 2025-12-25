package com.vyra.virtual_your_assets.constant;

public class MemberActivityEvent {

    // REGISTER
    public static final String ATTEMPT_REGISTER = "Member attempted to register an account";
    public static final String SUCCESS_REGISTER = "Member successfully registered an account";
    public static final String ATTEMPT_GENERATE_OTP_REGISTER = "Member requested OTP for register";
    public static final String SUCCESS_GENERATE_OTP_REGISTER = "OTP for register was successfully generated";

    // LOGIN
    public static final String ATTEMPT_LOGIN = "Member attempted to log in";
    public static final String SUCCESS_LOGIN = "Member successfully logged in";

    // RESEND OTP
    public static final String ATTEMPT_RESEND_OTP = "Member requested to resend OTP";
    public static final String SUCCESS_RESEND_OTP = "OTP was successfully sent to the user";

    // VERIFY OTP
    public static final String ATTEMPT_VERIFY_OTP = "Member attempted to verify OTP";
    public static final String SUCCESS_VERIFY_OTP = "OTP was successfully verified";

    // FORGOT PASSWORD
    public static final String ATTEMPT_GENERATE_FORGOT_PASSWORD = "Member requested OTP for password reset";
    public static final String SUCCESS_GENERATE_FORGOT_PASSWORD = "OTP for password reset was successfully generated";

    // RESET PASSWORD
    public static final String ATTEMPT_RESET_PASSWORD = "Member attempted to reset the password";
    public static final String SUCCESS_RESET_PASSWORD = "Member successfully reset the password";

}
