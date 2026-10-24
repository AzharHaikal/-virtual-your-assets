package com.vyra.be_virtual_your_assets.dto.habit;

import lombok.Data;

import java.time.YearMonth;
import java.util.List;

/** One month of a habit, day by day. */
@Data
public class HabitCalendarResponse {
    private String habitId;
    private YearMonth month;
    private int targetCount;
    /** Days that count, up to today: DONE, PARTIAL or MISSED (a pending today does not count yet). */
    private int scheduledDays;
    private int doneDays;
    private List<HabitCalendarDayResponse> days;
}
