package com.vyra.be_virtual_your_assets.dto.mood;

import lombok.Data;

import java.time.LocalDate;
import java.util.List;

/** How often each tag was used in [from, to] (descriptive only). */
@Data
public class MoodTagsSummaryResponse {
    private LocalDate from;
    private LocalDate to;
    private int entries;
    /** Most used first; unused tags are left out. */
    private List<MoodTagCountResponse> tags;
}
