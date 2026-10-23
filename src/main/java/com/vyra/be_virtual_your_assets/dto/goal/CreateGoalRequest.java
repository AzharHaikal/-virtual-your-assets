package com.vyra.be_virtual_your_assets.dto.goal;

import com.vyra.be_virtual_your_assets.constant.GoalCategory;
import com.vyra.be_virtual_your_assets.constant.GoalProgressType;
import com.vyra.be_virtual_your_assets.dto.category.CategoryRules;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Data
public class CreateGoalRequest {
    @NotBlank(message = "Title is required")
    @Size(max = 80, message = "Title must not exceed 80 characters")
    private String title;

    @Size(max = 1000, message = "Description must not exceed 1000 characters")
    private String description;

    @NotNull(message = "Category is required")
    private GoalCategory category;

    @NotBlank(message = "Icon is required")
    @Pattern(regexp = CategoryRules.ICON_PATTERN, message = "Invalid icon")
    private String icon;

    @NotBlank(message = "Color is required")
    @Pattern(regexp = CategoryRules.COLOR_PATTERN, message = "Color must be #RRGGBB")
    private String color;

    @NotNull(message = "Progress type is required")
    private GoalProgressType progressType;

    /** Required for VALUE, not allowed for MILESTONE. */
    @DecimalMin(value = "0.01", message = "Target must be greater than 0")
    @Digits(integer = 17, fraction = 2, message = "Invalid target")
    private BigDecimal targetValue;

    @Size(max = 20, message = "Unit must not exceed 20 characters")
    private String unit;

    /** Optional, the member's day; not before today. */
    private LocalDate dueDate;

    /** Optional first milestones, in order. */
    @Size(max = GoalRules.MAX_MILESTONES, message = "A goal can have up to 50 milestones")
    private List<@NotBlank(message = "Milestone title is required")
                 @Size(max = 120, message = "Milestone title must not exceed 120 characters") String> milestones = new ArrayList<>();
}
