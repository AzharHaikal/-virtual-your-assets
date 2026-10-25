package com.vyra.be_virtual_your_assets.dto.report.user;

import com.vyra.be_virtual_your_assets.constant.UserReportStatus;
import jakarta.validation.constraints.Size;
import lombok.Data;

/** Status, assignee (empty text = unassigned) and / or a note; only what is sent changes. */
@Data
public class UpdateUserReportRequest {
    private UserReportStatus status;

    /** Admin id; "" removes the assignee. */
    private String assignedAdminId;

    @Size(max = 1000, message = "Note must not exceed 1000 characters")
    private String note;
}
