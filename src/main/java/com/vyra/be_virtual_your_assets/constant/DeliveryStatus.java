package com.vyra.be_virtual_your_assets.constant;

/** State of a notification's delivery outside the app (push). */
public enum DeliveryStatus {
    /** Waiting for its time (after quiet hours) or for a retry. */
    PENDING,
    SENT,
    /** Not sent on purpose: no longer relevant, channel turned off, no device. */
    SKIPPED,
    /** Gave up after the retries. */
    FAILED
}
