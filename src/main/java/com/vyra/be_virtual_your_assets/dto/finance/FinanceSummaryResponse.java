package com.vyra.be_virtual_your_assets.dto.finance;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * Income and expense of the member in [from, to] (their own local dates), per category, and the
 * previous period of the same length for comparison. All amounts are positive; net = income - expense.
 */
@Data
public class FinanceSummaryResponse {
    private LocalDate from;
    private LocalDate to;
    private BigDecimal totalIncome;
    private BigDecimal totalExpense;
    private BigDecimal net;
    private LocalDate previousFrom;
    private LocalDate previousTo;
    private BigDecimal previousIncome;
    private BigDecimal previousExpense;
    /** Biggest first. */
    private List<CategoryAmountResponse> expenseByCategory;
    private List<CategoryAmountResponse> incomeByCategory;
}
