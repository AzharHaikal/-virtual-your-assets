package com.vyra.be_virtual_your_assets.dto.wallet;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class CreateWalletResponse {
    private String memberId;
    private String phoneNumber;
    private BigDecimal balance;
}
