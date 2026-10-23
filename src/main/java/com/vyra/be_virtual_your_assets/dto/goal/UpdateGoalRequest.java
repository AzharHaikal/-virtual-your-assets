package com.vyra.be_virtual_your_assets.dto.goal;

import com.vyra.be_virtual_your_assets.constant.GoalCategory;
import com.vyra.be_virtual_your_assets.constant.GoalStatus;
import com.vyra.be_virtual_your_assets.dto.category.CategoryRules;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

/** Only the fields that are sent change. The progress type cannot change. */
@Data
public class UpdateGoalRequest {
    @Size(min = 1, max = 80, message = "Title must be 1 to 80 characters")
    private String title;

    /** An empty text removes the description. */
    @Size(max = 1000, message = "Description must not exceed 1000 characters")
    private String description;

    private GoalCategory category;

    @Pattern(regexp = CategoryRules.ICON_PATTERN, message = "Invalid icon")
    private String icon;

    @Pattern(regexp = CategoryRules.COLOR_PATTERN, message = "Color must be #RRGGBB")
    private String color;

    /** Only for VALUE goals. */
    @DecimalMin(value = "0.01", message = "Target must be greater than 0")
    @Digits(integer = 17, fraction = 2, message = "Invalid target")
    private BigDecimal targetValue;

    /** An empty text removes the unit. */
    @Size(max = 20, message = "Unit must not exceed 20 characters")
    private String unit;

    private LocalDate dueDate;

    /** true removes the due date. */
    private boolean clearDueDate;

    /** COMPLETED sets completedAt; any other status clears it. */
    private GoalStatus status;
}
