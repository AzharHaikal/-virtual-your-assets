package com.vyra.virtual_your_assets.dto.member;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class GetMemberResponse {
    private String firstName;
    private String lastName;
    private String fullName;
    private String email;
    private String phoneNumber;
    private BigDecimal totalIncome;
    private BigDecimal totalExpense;
    private BigDecimal balance;
    private BigDecimal growthPercentage;
}
