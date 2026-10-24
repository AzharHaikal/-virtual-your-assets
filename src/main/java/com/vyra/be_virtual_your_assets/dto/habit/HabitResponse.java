package com.vyra.be_virtual_your_assets.dto.habit;

import com.vyra.be_virtual_your_assets.constant.HabitCategory;
import com.vyra.be_virtual_your_assets.constant.HabitScheduleType;
import lombok.Data;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.List;

@Data
public class HabitResponse {
    private String id;
    private String name;
    private HabitCategory category;
    private String icon;
    private String color;
    private int targetCount;
    private HabitScheduleType scheduleType;
    private List<DayOfWeek> scheduleDays;
    private LocalDate startDate;
    private boolean archived;

    private boolean scheduledToday;
    private int todayCount;
    private boolean doneToday;

    /** Scheduled days of this week that are done / scheduled days of this week (unscheduled days never count). */
    private int weekDone;
    private int weekScheduled;
    private List<HabitDayResponse> week;

    /** Scheduled days done in a row up to today (today not done yet does not break it). */
    private int currentStreak;
    /** Longest such run in the last 400 days. */
    private int bestStreak;
}
