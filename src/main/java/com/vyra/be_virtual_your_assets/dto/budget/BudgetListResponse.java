package com.vyra.be_virtual_your_assets.dto.budget;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;

@Data
public class BudgetListResponse {
    private YearMonth month;
    private LocalDate periodStart;
    private LocalDate periodEnd;
    private BigDecimal totalBudget;
    private BigDecimal totalSpent;
    private BigDecimal totalRemaining;
    /** In the order the budgets were added. */
    private List<BudgetResponse> items;
}
