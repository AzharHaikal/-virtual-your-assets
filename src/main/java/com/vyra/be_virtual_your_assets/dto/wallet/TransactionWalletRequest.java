package com.vyra.be_virtual_your_assets.dto.wallet;

import com.vyra.be_virtual_your_assets.constant.transaction.TransactionType;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class TransactionWalletRequest {
    private String transactionId;
    private String phoneNumber;
    private BigDecimal amount;
    private TransactionType transactionType;
}
