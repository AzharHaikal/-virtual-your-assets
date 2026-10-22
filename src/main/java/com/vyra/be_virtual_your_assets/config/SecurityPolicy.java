package com.vyra.be_virtual_your_assets.config;

import lombok.Getter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Tunable brute-force protection settings (see application.yaml, {@code vyra.security.*}).
 * Defaults are used when a property is absent, so the class also works as a plain object in tests.
 */
@Getter
@Component
public class SecurityPolicy {

    /** Wrong PIN attempts (login, change PIN, contact change) before the member is locked. */
    @Value("${vyra.security.pin.max-attempts:5}")
    private int pinMaxAttempts = 5;

    @Value("${vyra.security.pin.lock-minutes:15}")
    private int pinLockMinutes = 15;

    /** Lock applied after the OTP attempt limit is reached (replaces suspending the account). */
    @Value("${vyra.security.otp.lock-minutes:10}")
    private int otpLockMinutes = 10;

    /** Minimum time between two OTP e-mails of the same type to the same member. */
    @Value("${vyra.security.otp.resend-cooldown-seconds:60}")
    private int otpResendCooldownSeconds = 60;

    @Value("${vyra.security.admin-login.max-attempts:5}")
    private int adminMaxAttempts = 5;

    @Value("${vyra.security.admin-login.lock-minutes:15}")
    private int adminLockMinutes = 15;
}
