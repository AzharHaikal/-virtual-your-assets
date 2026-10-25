package com.vyra.be_virtual_your_assets.dto.report.user;

import com.vyra.be_virtual_your_assets.constant.UserReportCategory;
import com.vyra.be_virtual_your_assets.constant.UserReportStatus;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/**
 * A report. The member sees their own (without admin details); the portal also gets the reporter name
 * and e-mail to follow up, the assignee and the handling history.
 */
@Data
public class UserReportResponse {
    private String id;
    private UserReportCategory category;
    private String description;
    private boolean hasAttachment;
    private UserReportStatus status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private LocalDateTime resolvedAt;

    // Portal only
    private String memberId;
    private String memberName;
    private String memberEmail;
    private String appVersion;
    private String platform;
    private String assignedAdminId;
    private String assignedAdminName;
    private List<Note> history;

    @Data
    public static class Note {
        private String adminName;
        private UserReportStatus status;
        private String note;
        private LocalDateTime createdAt;
    }
}
