package com.vyra.be_virtual_your_assets.dto.goal;

import com.vyra.be_virtual_your_assets.constant.GoalCategory;
import com.vyra.be_virtual_your_assets.constant.GoalProgressType;
import com.vyra.be_virtual_your_assets.constant.GoalStatus;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class GoalResponse {
    private String id;
    private String title;
    private String description;
    private GoalCategory category;
    private String icon;
    private String color;
    private GoalProgressType progressType;
    private BigDecimal targetValue;
    private BigDecimal currentValue;
    private String unit;
    private LocalDate dueDate;
    private GoalStatus status;
    private LocalDateTime completedAt;
    private LocalDateTime createdAt;
    /** A savings target shown in the finance overview. */
    private boolean linkedToFinance;

    /** 0..100, computed on the server: milestones done / total, or current / target. */
    private int progressPercent;
    private int milestoneDone;
    private int milestoneTotal;
    /** Active and the due date has passed (informational; the status does not change). */
    private boolean overdue;

    private List<GoalMilestoneResponse> milestones;
    /** Latest 20 entries, newest first; only in the detail and after a change. */
    private List<GoalProgressLogResponse> progressLogs;
}
