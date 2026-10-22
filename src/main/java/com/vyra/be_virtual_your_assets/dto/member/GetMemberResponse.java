package com.vyra.be_virtual_your_assets.dto.member;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class GetMemberResponse {
    private String firstName;
    private String lastName;
    private String fullName;
    private String email;
    private String phoneNumber;
    /** IANA time zone used for the member's days (charts, history filters). */
    private String timezone;
    private BigDecimal totalIncome;
    private BigDecimal totalExpense;
    private BigDecimal balance;
    private BigDecimal growthPercentage;
}
