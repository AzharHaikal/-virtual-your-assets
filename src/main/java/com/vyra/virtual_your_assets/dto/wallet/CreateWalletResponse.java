package com.vyra.virtual_your_assets.dto.wallet;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class CreateWalletResponse {
    private String memberId;
    private String phoneNumber;
    private BigDecimal balance;
}
