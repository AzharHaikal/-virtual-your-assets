package com.vyra.be_virtual_your_assets.dto.home;

import com.vyra.be_virtual_your_assets.dto.finance.CategoryAmountResponse;
import com.vyra.be_virtual_your_assets.dto.goal.GoalResponse;
import com.vyra.be_virtual_your_assets.dto.habit.HabitResponse;
import com.vyra.be_virtual_your_assets.dto.mood.MoodEntryResponse;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * Everything the member's home screen shows, in one call. A module without data is empty or null
 * (never a fake 0), so the app can show an invitation instead.
 */
@Data
public class HomeSummaryResponse {
    private String firstName;
    /** The member's day. */
    private LocalDate today;

    private Finance finance;
    private Mood mood;
    private Habits habits;
    private Goals goals;
    /** Latest activity of all modules, newest first (max 8). */
    private List<HomeActivityResponse> recentActivity;

    @Data
    public static class Finance {
        /** null when the member has no wallet yet. */
        private BigDecimal balance;
        /** This calendar month in the member's zone. */
        private LocalDate from;
        private LocalDate to;
        private BigDecimal totalIncome;
        private BigDecimal totalExpense;
        private BigDecimal net;
        private BigDecimal previousExpense;
        /** false when there is no transaction this month: show an invitation, not zeros. */
        private boolean hasTransactions;
        /** Biggest 3 expense categories of the month. */
        private List<CategoryAmountResponse> topExpenseCategories;
    }

    @Data
    public static class Mood {
        /** null when not checked in yet today; never carries the journal ({@code note} is null). */
        private MoodEntryResponse today;
        /** Average of the last 7 days (today included), one decimal; null without entries. */
        private BigDecimal weekAverage;
        private int weekEntries;
    }

    @Data
    public static class Habits {
        /** Active habits scheduled today: done / total. total 0 = no habit today. */
        private int doneToday;
        private int totalToday;
        /** false when the member has no active habit at all. */
        private boolean hasHabits;
        private List<HabitResponse> today;
    }

    @Data
    public static class Goals {
        private int activeCount;
        /** Up to 3 active goals, nearest due date first. */
        private List<GoalResponse> top;
    }
}
