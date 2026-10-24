package com.vyra.be_virtual_your_assets.dto.insight;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

/** One calendar week (Monday .. Sunday, member's days) of habits, mood and spending together. */
@Data
public class WeeklyInsightResponse {
    private LocalDate from;
    private LocalDate to;

    private Habits habits;
    private Mood mood;
    private Expense expense;

    @Data
    public static class Habits {
        /**
         * Scheduled days of the active habits up to today (today only once done; unscheduled days never
         * count) and how many of them were done.
         */
        private int scheduled;
        private int done;
        /** done / scheduled in whole percent; null when nothing was scheduled. */
        private Integer percent;
    }

    @Data
    public static class Mood {
        /** 2 decimals; null without entries. */
        private BigDecimal average;
        private int entries;
    }

    @Data
    public static class Expense {
        private BigDecimal thisWeek;
        private BigDecimal lastWeek;
        /** (this - last) / last in percent, 1 decimal; null when last week had no expenses. */
        private BigDecimal changePercent;
    }
}
