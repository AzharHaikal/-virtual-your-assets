package com.vyra.be_virtual_your_assets.dto.dashboard;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class DashboardSummaryResponse {
    private BigDecimal totalBalance;
    private BigDecimal totalIncome;
    private BigDecimal totalExpense;
    private long totalMembers;
    private long activeMembers;
    private long newMembersToday;
    private long totalTransactions;
}
