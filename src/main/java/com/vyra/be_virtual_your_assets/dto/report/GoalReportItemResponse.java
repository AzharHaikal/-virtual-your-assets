package com.vyra.be_virtual_your_assets.dto.report;

import com.vyra.be_virtual_your_assets.constant.GoalCategory;
import com.vyra.be_virtual_your_assets.constant.GoalProgressType;
import com.vyra.be_virtual_your_assets.constant.GoalStatus;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/** One goal for the portal Goal report (SUPER_ADMIN only). */
@Data
public class GoalReportItemResponse {
    private String goalId;
    private String memberName;
    private String memberEmail;
    private String memberPhoneNumber;
    private String title;
    private GoalCategory category;
    private GoalProgressType progressType;
    private BigDecimal currentValue;
    private BigDecimal targetValue;
    private String unit;
    private int milestonesDone;
    private int milestonesTotal;
    /** 0..100, same rule as the app. */
    private int progressPercent;
    private GoalStatus status;
    private LocalDate dueDate;
    private LocalDateTime createdAt;
    private LocalDateTime completedAt;
}
