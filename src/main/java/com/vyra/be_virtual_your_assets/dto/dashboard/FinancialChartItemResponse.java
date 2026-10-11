package com.vyra.be_virtual_your_assets.dto.dashboard;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class FinancialChartItemResponse {
    private String label;
    private BigDecimal income;
    private BigDecimal expense;
}
