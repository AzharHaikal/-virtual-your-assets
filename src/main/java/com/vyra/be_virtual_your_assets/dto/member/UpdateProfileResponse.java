package com.vyra.be_virtual_your_assets.dto.member;

import lombok.Data;

@Data
public class UpdateProfileResponse {
    private String firstName;
    private String lastName;
    private String email;
    private String phoneNumber;

    /** True when an e-mail / phone change was requested and still waits for its OTP. */
    private boolean contactChangePending;

    /** Masked e-mail address the OTP was sent to (only when contactChangePending). */
    private String verificationSentTo;
}
