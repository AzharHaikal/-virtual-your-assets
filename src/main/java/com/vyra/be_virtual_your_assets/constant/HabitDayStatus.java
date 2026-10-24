package com.vyra.be_virtual_your_assets.constant;

/** A day of a habit's calendar (member's local days). */
public enum HabitDayStatus {
    /** Target reached. */
    DONE,
    /** Scheduled and some progress, but the target was not reached. */
    PARTIAL,
    /** Scheduled, in the past, nothing done. */
    MISSED,
    /** Today, scheduled and not done yet (not missed). */
    PENDING,
    /** Not scheduled on that weekday. */
    NOT_SCHEDULED,
    /** Before the habit started, or in the future. */
    OUT_OF_RANGE
}
