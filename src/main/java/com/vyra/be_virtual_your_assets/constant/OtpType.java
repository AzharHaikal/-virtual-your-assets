package com.vyra.be_virtual_your_assets.constant;

public enum OtpType {
    REGISTER,
    FORGOT_PIN,
    LOGIN,
    /** Confirms a new e-mail address / phone number; only used through the authenticated profile flow. */
    CHANGE_CONTACT,
    /** Confirms that the member deletes their own account. */
    DELETE_ACCOUNT
}