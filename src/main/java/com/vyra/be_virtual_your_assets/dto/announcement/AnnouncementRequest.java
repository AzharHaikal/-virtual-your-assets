package com.vyra.be_virtual_your_assets.dto.announcement;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

/** Create or replace a draft / published announcement. Times are ISO-8601 with offset (or UTC). */
@Data
public class AnnouncementRequest {
    @NotBlank(message = "Title is required")
    @Size(max = 120, message = "Title must not exceed 120 characters")
    private String title;

    @NotBlank(message = "Message is required")
    @Size(max = 1000, message = "Message must not exceed 1000 characters")
    private String body;

    /** ALL (default), ID or EN. */
    @Pattern(regexp = "^(ALL|ID|EN)$", message = "Language must be ALL, ID or EN")
    private String language = "ALL";

    @NotNull(message = "Start is required")
    private String startsAt;

    /** Optional; empty = until unpublished. */
    private String endsAt;
}
