package com.vyra.virtual_your_assets.dto.transaction;

import com.vyra.virtual_your_assets.constant.transaction.TransactionCategory;
import com.vyra.virtual_your_assets.constant.transaction.TransactionStatus;
import com.vyra.virtual_your_assets.constant.transaction.TransactionType;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class TransactionHistoryResponse {
    private String transactionId;
    private String referenceNumber;
    private TransactionType type;
    private String category;
    private TransactionStatus status;
    private BigDecimal amount;
    private String transactionDesc;
    private LocalDateTime transactionDate;

}
