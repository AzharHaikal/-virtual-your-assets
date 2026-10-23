package com.vyra.be_virtual_your_assets.dto.goal;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class GoalProgressLogResponse {
    private String id;
    private BigDecimal delta;
    private BigDecimal valueAfter;
    private String note;
    private LocalDateTime createdAt;
}
