package com.vyra.be_virtual_your_assets.dto.goal;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class GoalMilestoneResponse {
    private String id;
    private String title;
    private int position;
    private boolean completed;
    private LocalDateTime completedAt;
}
