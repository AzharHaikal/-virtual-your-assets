package com.vyra.virtual_your_assets.dto.wallet;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class GetMemberWalletResponse {
    private String phoneNumber;
    private BigDecimal totalCredit;
    private BigDecimal totalDebit;
    private BigDecimal balance;
}
