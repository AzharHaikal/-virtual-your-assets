package com.vyra.be_virtual_your_assets.dto.habit;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.Data;

/** Progress of the day; without a body (or without count) the habit is marked fully done. */
@Data
public class HabitCompletionRequest {
    @Min(value = 1, message = "Count must be between 1 and 1000")
    @Max(value = 1000, message = "Count must be between 1 and 1000")
    private Integer count;
}
