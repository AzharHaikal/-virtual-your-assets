package com.vyra.be_virtual_your_assets.dto.chart;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ChartItemResponse {
    private String label;
    private BigDecimal income;
    private BigDecimal expense;

}