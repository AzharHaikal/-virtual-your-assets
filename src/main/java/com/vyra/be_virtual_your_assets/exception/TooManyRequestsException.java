package com.vyra.be_virtual_your_assets.exception;

import com.vyra.be_virtual_your_assets.constant.ErrorConstant;
import lombok.Getter;

/**
 * Thrown when a caller must slow down: a temporary account lock-out, an OTP cool-down
 * or a request rate limit. Mapped to HTTP 429 (with a {@code Retry-After} header) by
 * {@code GlobalExceptionHandler}. The message is the constant's text followed by the
 * remaining wait, e.g. "... Please try again in 14 minutes."
 */
@Getter
public class TooManyRequestsException extends RuntimeException {
    private final ErrorConstant errorConstant;
    private final long retryAfterSeconds;

    public TooManyRequestsException(ErrorConstant errorConstant, long retryAfterSeconds) {
        super(errorConstant.getMessage() + " " + humanize(retryAfterSeconds) + ".");
        this.errorConstant = errorConstant;
        this.retryAfterSeconds = Math.max(1, retryAfterSeconds);
    }

    /** 1..59 s gives "N seconds"; otherwise whole minutes rounded up. */
    public static String humanize(long seconds) {
        long s = Math.max(1, seconds);
        if (s < 60) return s + (s == 1 ? " second" : " seconds");
        long minutes = (s + 59) / 60;
        return minutes + (minutes == 1 ? " minute" : " minutes");
    }
}
