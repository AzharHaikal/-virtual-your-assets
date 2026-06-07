package com.vyra.virtual_your_assets.dto.transaction;

import com.vyra.virtual_your_assets.constant.transaction.TransactionCategory;
import com.vyra.virtual_your_assets.constant.transaction.TransactionType;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class CreateTransactionResponse {
    private String transactionId;
    private String phoneNumber;
    private String email;
    private String category;
    private TransactionType transactionType;
    private BigDecimal amount;
    private String referenceNumber;
    private String transactionDesc;
    private BigDecimal balance;
}
