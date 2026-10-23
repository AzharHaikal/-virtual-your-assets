package com.vyra.be_virtual_your_assets.dto.mood;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class CreateMoodTagRequest {
    @NotBlank(message = "Name is required")
    @Size(max = 40, message = "Name must not exceed 40 characters")
    private String name;
}
