package com.vyra.be_virtual_your_assets.dto.insight;

import com.vyra.be_virtual_your_assets.constant.HabitCategory;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * Descriptive analytics for the member themself: mood on days a habit was done vs. not done (last 90
 * days), income/expense of the last 12 months and when value goals will be reached at the recent pace.
 * Correlations are descriptive only, not causes.
 */
@Data
public class AdvancedInsightResponse {
    private LocalDate from;
    private LocalDate to;
    private List<HabitMood> habitMood;
    private List<Month> months;
    private List<GoalProjection> goals;

    /** Only habits with at least {@code minDays} mood check-ins on done and on missed scheduled days. */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class HabitMood {
        private String habitId;
        private String name;
        private HabitCategory category;
        private int doneDays;
        private int missedDays;
        private BigDecimal moodWhenDone;
        private BigDecimal moodWhenMissed;
        private BigDecimal difference;
    }

    /** month = yyyy-MM; amounts positive, net = income - expense. */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Month {
        private String month;
        private BigDecimal income;
        private BigDecimal expense;
        private BigDecimal net;
    }

    /**
     * Pace = progress added in the last 90 days (or since the goal was created) per 30 days.
     * projectedDate null when there is no positive pace; onTrack null without a due date.
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class GoalProjection {
        private String goalId;
        private String title;
        private boolean linkedToFinance;
        private BigDecimal targetValue;
        private BigDecimal currentValue;
        private String unit;
        private BigDecimal pacePerMonth;
        private LocalDate projectedDate;
        private LocalDate dueDate;
        private Boolean onTrack;
    }
}
