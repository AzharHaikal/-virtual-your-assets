package com.vyra.be_virtual_your_assets.dto.category;

import com.vyra.be_virtual_your_assets.constant.transaction.TransactionType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class CreateCategoryRequest {
    @NotBlank(message = "Name is required")
    @Size(max = 50, message = "Name must not exceed 50 characters")
    private String name;

    @NotNull(message = "Type is required")
    private TransactionType type;

    @NotBlank(message = "Icon is required")
    @Pattern(regexp = CategoryRules.ICON_PATTERN, message = "Invalid icon")
    private String icon;

    @NotBlank(message = "Color is required")
    @Pattern(regexp = CategoryRules.COLOR_PATTERN, message = "Color must be #RRGGBB")
    private String color;
}
