package com.vyra.be_virtual_your_assets.dto.finance;

import lombok.Data;

import java.math.BigDecimal;

/** Total of one category in a period (always positive) and its share of the type's total. */
@Data
public class CategoryAmountResponse {
    private String categoryId;
    private String code;
    private String name;
    private String icon;
    private String color;
    private BigDecimal amount;
    /** 0-100, one decimal. */
    private BigDecimal percentage;
}
