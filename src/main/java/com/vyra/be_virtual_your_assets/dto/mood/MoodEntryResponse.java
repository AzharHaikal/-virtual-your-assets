package com.vyra.be_virtual_your_assets.dto.mood;

import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class MoodEntryResponse {
    private String id;
    /** The member's day of this entry. */
    private LocalDate localDate;
    /** 1 (very bad) .. 5 (very good). */
    private int moodScore;
    private String note;
    private List<MoodTagResponse> tags;
    private LocalDateTime checkedInAt;
}
