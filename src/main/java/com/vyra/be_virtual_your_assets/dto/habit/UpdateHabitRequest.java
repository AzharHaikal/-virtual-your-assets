package com.vyra.be_virtual_your_assets.dto.habit;

import com.vyra.be_virtual_your_assets.constant.HabitCategory;
import com.vyra.be_virtual_your_assets.constant.HabitScheduleType;
import com.vyra.be_virtual_your_assets.dto.category.CategoryRules;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.List;

/** Only the fields that are sent change. */
@Data
public class UpdateHabitRequest {
    @Size(min = 1, max = 60, message = "Name must be 1 to 60 characters")
    private String name;

    private HabitCategory category;

    @Pattern(regexp = CategoryRules.ICON_PATTERN, message = "Invalid icon")
    private String icon;

    @Pattern(regexp = CategoryRules.COLOR_PATTERN, message = "Color must be #RRGGBB")
    private String color;

    @Min(value = 1, message = "Target must be between 1 and 100")
    @Max(value = 100, message = "Target must be between 1 and 100")
    private Integer targetCount;

    private HabitScheduleType scheduleType;

    private List<DayOfWeek> scheduleDays;

    private Boolean reminderEnabled;

    /** HH:mm, the member's local time. */
    private LocalTime reminderTime;
}
