package com.vyra.be_virtual_your_assets.dto.goal;

import jakarta.validation.constraints.Size;
import lombok.Data;

/** Only the fields that are sent change. */
@Data
public class UpdateMilestoneRequest {
    @Size(min = 1, max = 120, message = "Milestone title must be 1 to 120 characters")
    private String title;

    private Boolean completed;
}
