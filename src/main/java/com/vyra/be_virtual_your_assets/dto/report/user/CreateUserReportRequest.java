package com.vyra.be_virtual_your_assets.dto.report.user;

import com.vyra.be_virtual_your_assets.constant.UserReportCategory;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.ToString;

@Data
public class CreateUserReportRequest {
    @NotNull(message = "Category is required")
    private UserReportCategory category;

    @NotBlank(message = "Describe the problem")
    @Size(max = 2000, message = "Description must not exceed 2000 characters")
    private String description;

    /** Optional screenshot, base64 (JPEG / PNG / WebP, at most 1 MB once decoded). */
    @Size(max = 1_500_000, message = "The attachment is too large")
    @ToString.Exclude
    private String attachmentBase64;

    @Size(max = 30, message = "Invalid app version")
    private String appVersion;

    @Size(max = 20, message = "Invalid platform")
    private String platform;
}
