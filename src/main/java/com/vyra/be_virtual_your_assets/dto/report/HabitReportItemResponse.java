package com.vyra.be_virtual_your_assets.dto.report;

import com.vyra.be_virtual_your_assets.constant.HabitCategory;
import lombok.Data;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.List;

/** One habit with its completions in the report period, for the portal Habit report (SUPER_ADMIN only). */
@Data
public class HabitReportItemResponse {
    private String habitId;
    private String memberName;
    private String memberEmail;
    private String memberPhoneNumber;
    private String name;
    private HabitCategory category;
    /** Times per day to count the day as done. */
    private int targetCount;
    private List<DayOfWeek> scheduleDays;
    private LocalDate startDate;
    private boolean archived;
    /** Scheduled days in the period (from the start date, until the end of the period or the archive day). */
    private int scheduledDays;
    /** Days in the period on which the target was reached. */
    private int completedDays;
    /** completedDays / scheduledDays, 0..100, rounded down; 0 without a scheduled day. */
    private int completionRate;
    /** Last day in the period with any progress; null without one. */
    private LocalDate lastActivityDate;
}
