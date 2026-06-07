package com.vyra.be_virtual_your_assets.dto.wallet;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class GetMemberWalletResponse {
    private String phoneNumber;
    private BigDecimal totalIncome;
    private BigDecimal totalExpense;
    private BigDecimal balance;
}
