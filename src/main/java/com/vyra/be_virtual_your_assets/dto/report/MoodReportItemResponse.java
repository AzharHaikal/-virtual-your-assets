package com.vyra.be_virtual_your_assets.dto.report;

import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * One mood check-in for the portal Mood report (SUPER_ADMIN only), including the member's journal
 * note (stored encrypted, decrypted for the Super Admin).
 */
@Data
public class MoodReportItemResponse {
    private String entryId;
    private String memberName;
    private String memberEmail;
    private String memberPhoneNumber;
    private LocalDate date;
    /** 1 (very bad) .. 5 (very good). */
    private int moodScore;
    private List<String> tags;
    /** The member's journal note for that day; null when none was written. */
    private String note;
    private LocalDateTime checkedInAt;
}
