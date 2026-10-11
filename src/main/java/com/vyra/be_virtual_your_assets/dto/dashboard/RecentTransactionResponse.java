package com.vyra.be_virtual_your_assets.dto.dashboard;

import com.vyra.be_virtual_your_assets.constant.transaction.TransactionStatus;
import com.vyra.be_virtual_your_assets.constant.transaction.TransactionType;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class RecentTransactionResponse {
    private String transactionId;
    private String referenceNumber;
    private String memberEmail;
    private String memberPhoneNumber;
    private String category;
    private TransactionType type;
    private TransactionStatus status;
    private BigDecimal amount;
    private LocalDateTime transactionDate;
}
