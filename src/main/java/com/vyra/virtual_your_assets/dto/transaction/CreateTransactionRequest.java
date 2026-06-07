package com.vyra.virtual_your_assets.dto.transaction;

import com.vyra.virtual_your_assets.constant.transaction.TransactionCategory;
import com.vyra.virtual_your_assets.constant.transaction.TransactionType;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class CreateTransactionRequest {
    private TransactionCategory category;
    private TransactionType type;
    @NotNull(message = "Amount is required")
    @Positive(message = "Amount must be greater than zero")
    private BigDecimal amount;
    private LocalDateTime transactionDate;
    private String transactionDesc;
}
