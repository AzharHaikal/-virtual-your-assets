package com.vyra.be_virtual_your_assets.dto.transaction;

import com.vyra.be_virtual_your_assets.constant.transaction.PaymentMethod;
import com.vyra.be_virtual_your_assets.constant.transaction.TransactionStatus;
import com.vyra.be_virtual_your_assets.constant.transaction.TransactionType;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
public class TransactionHistoryResponse {
    private String transactionId;
    private String referenceNumber;
    private TransactionType type;
    /** Legacy code (built-in code, or OTHERS_* for a custom category). */
    private String category;
    private String categoryId;
    private String categoryName;
    private String categoryIcon;
    private String categoryColor;
    private TransactionStatus status;
    private BigDecimal amount;
    private String transactionDesc;
    private PaymentMethod paymentMethod;
    private LocalDateTime transactionDate;
    /** The member's local date of the transaction (grouping by day in the app). */
    private LocalDate localDate;

}
