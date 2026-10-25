package com.vyra.be_virtual_your_assets.dto.announcement;

import com.vyra.be_virtual_your_assets.constant.AnnouncementStatus;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class AnnouncementResponse {
    private String id;
    private String title;
    private String body;
    private String language;
    private AnnouncementStatus status;
    private LocalDateTime startsAt;
    private LocalDateTime endsAt;
    private LocalDateTime publishedAt;
    /** When it was sent as a notification; portal only. */
    private LocalDateTime deliveredAt;
    /** Being shown to members right now. */
    private boolean active;
}
