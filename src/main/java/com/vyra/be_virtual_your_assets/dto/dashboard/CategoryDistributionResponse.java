package com.vyra.be_virtual_your_assets.dto.dashboard;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CategoryDistributionResponse {
    private String category;
    private BigDecimal totalAmount;
}
