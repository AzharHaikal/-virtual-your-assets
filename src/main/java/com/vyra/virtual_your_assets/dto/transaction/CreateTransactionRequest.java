package com.vyra.virtual_your_assets.dto.transaction;

import com.vyra.virtual_your_assets.constant.transaction.TransactionCategory;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class CreateTransactionRequest {
    private String phoneNumber;
    private String email;
    private TransactionCategory category;
    // private String transactionType;
    private BigDecimal amount;
    private String transactionDesc;
}
