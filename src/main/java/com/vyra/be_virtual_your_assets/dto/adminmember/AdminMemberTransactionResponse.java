package com.vyra.be_virtual_your_assets.dto.adminmember;

import com.vyra.be_virtual_your_assets.constant.transaction.TransactionStatus;
import com.vyra.be_virtual_your_assets.constant.transaction.TransactionType;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class AdminMemberTransactionResponse {
    private String transactionId;
    private String referenceNumber;
    private String category;
    private TransactionType type;
    private TransactionStatus status;
    private BigDecimal amount;
    private String description;
    private LocalDateTime transactionDate;
}
