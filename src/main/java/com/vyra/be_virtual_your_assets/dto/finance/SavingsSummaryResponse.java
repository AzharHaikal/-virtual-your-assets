package com.vyra.be_virtual_your_assets.dto.finance;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/** The member's active savings targets (goals linked to the finance overview), in IDR. */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class SavingsSummaryResponse {
    private long activeGoals;
    private BigDecimal totalTarget;
    private BigDecimal totalSaved;
}
