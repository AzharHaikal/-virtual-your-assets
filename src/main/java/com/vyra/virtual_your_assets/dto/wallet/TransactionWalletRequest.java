package com.vyra.virtual_your_assets.dto.wallet;

import com.vyra.virtual_your_assets.constant.transaction.TransactionCategory;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class TransactionWalletRequest {
    private String transactionId;
    private String phoneNumber;
    private BigDecimal amount;
    private TransactionCategory category;
}
