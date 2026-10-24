package com.vyra.be_virtual_your_assets.dto.habit;

import com.vyra.be_virtual_your_assets.constant.HabitCategory;
import com.vyra.be_virtual_your_assets.constant.HabitScheduleType;
import com.vyra.be_virtual_your_assets.dto.category.CategoryRules;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

@Data
public class CreateHabitRequest {
    @NotBlank(message = "Name is required")
    @Size(max = 60, message = "Name must not exceed 60 characters")
    private String name;

    @NotNull(message = "Category is required")
    private HabitCategory category;

    @NotBlank(message = "Icon is required")
    @Pattern(regexp = CategoryRules.ICON_PATTERN, message = "Invalid icon")
    private String icon;

    @NotBlank(message = "Color is required")
    @Pattern(regexp = CategoryRules.COLOR_PATTERN, message = "Color must be #RRGGBB")
    private String color;

    /** Times per day, e.g. 8 glasses of water; 1 = done / not done. */
    @Min(value = 1, message = "Target must be between 1 and 100")
    @Max(value = 100, message = "Target must be between 1 and 100")
    private Integer targetCount = 1;

    @NotNull(message = "Schedule is required")
    private HabitScheduleType scheduleType;

    /** Required for SPECIFIC_DAYS, ignored for DAILY. */
    private List<DayOfWeek> scheduleDays = new ArrayList<>();

    /** Daily reminder (on scheduled days, while not done) at {@link #reminderTime}, the member's local time. */
    private boolean reminderEnabled;

    /** HH:mm; required when the reminder is enabled. */
    private LocalTime reminderTime;
}
