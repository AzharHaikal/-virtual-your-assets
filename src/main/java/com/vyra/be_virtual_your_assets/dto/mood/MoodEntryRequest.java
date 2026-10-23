package com.vyra.be_virtual_your_assets.dto.mood;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/** Check-in of one day: replaces the day's entry if there is one. */
@Data
public class MoodEntryRequest {
    @NotNull(message = "Mood is required")
    @Min(value = 1, message = "Mood must be between 1 and 5")
    @Max(value = 5, message = "Mood must be between 1 and 5")
    private Integer moodScore;

    @Size(max = 2000, message = "Journal must not exceed 2000 characters")
    private String note;

    @Size(max = 10, message = "Choose at most 10 tags")
    private List<String> tagIds = new ArrayList<>();
}
