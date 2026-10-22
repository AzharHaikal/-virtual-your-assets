package com.vyra.be_virtual_your_assets.dto.settings;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class UpdateSystemConfigRequest {
    @NotBlank(message = "Value is required")
    @Size(max = 500, message = "Value must be at most 500 characters")
    private String value;
}
