package com.vyra.be_virtual_your_assets.dto.budget;

import com.vyra.be_virtual_your_assets.constant.BudgetLevel;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

/** A budget with what is spent of it so far (SUCCESS expenses of the category in the period, deleted ones excluded). */
@Data
public class BudgetResponse {
    private String id;
    private String categoryId;
    private String categoryCode;
    private String categoryName;
    private String categoryIcon;
    private String categoryColor;
    private LocalDate periodStart;
    private LocalDate periodEnd;
    private BigDecimal amount;
    private BigDecimal spent;
    /** amount - spent; negative when over the budget. */
    private BigDecimal remaining;
    /** spent / amount in whole percent, rounded down; can be above 100. */
    private int percent;
    /** WARNING from 80%, OVER above 100%. */
    private BudgetLevel level;
}
