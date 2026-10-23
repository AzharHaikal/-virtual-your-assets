package com.vyra.be_virtual_your_assets.dto.goal;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class CreateMilestoneRequest {
    @NotBlank(message = "Milestone title is required")
    @Size(max = 120, message = "Milestone title must not exceed 120 characters")
    private String title;
}
