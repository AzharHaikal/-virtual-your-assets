package com.vyra.be_virtual_your_assets.constant;

import com.vyra.be_virtual_your_assets.exception.BadRequestException;

import java.util.Locale;

/** week = the 7 days of a calendar week (Monday .. Sunday); month = a calendar month, one point per week. */
public enum TrendPeriod {
    WEEK,
    MONTH;

    /** Case-insensitive ("week", "MONTH"); default WEEK. */
    public static TrendPeriod parse(String value) {
        if (value == null || value.isBlank()) {
            return WEEK;
        }
        try {
            return valueOf(value.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            throw new BadRequestException(ErrorConstant.INVALID_TREND_PERIOD);
        }
    }
}
