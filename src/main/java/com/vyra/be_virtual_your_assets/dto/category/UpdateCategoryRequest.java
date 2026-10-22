package com.vyra.be_virtual_your_assets.dto.category;

import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

/** PATCH semantics: null keeps the current value. The type cannot change (transactions depend on it). */
@Data
public class UpdateCategoryRequest {
    @Size(min = 1, max = 50, message = "Name must be 1 to 50 characters")
    private String name;

    @Pattern(regexp = CategoryRules.ICON_PATTERN, message = "Invalid icon")
    private String icon;

    @Pattern(regexp = CategoryRules.COLOR_PATTERN, message = "Color must be #RRGGBB")
    private String color;
}
