package com.vyra.be_virtual_your_assets.dto.transaction;

import com.vyra.be_virtual_your_assets.constant.transaction.TransactionCategory;
import com.vyra.be_virtual_your_assets.constant.transaction.TransactionType;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.math.BigDecimal;

/** PATCH semantics: a null field keeps the current value. */
@Data
public class UpdateTransactionRequest {
    private TransactionType type;

    private TransactionCategory category;

    /** Always positive; the sign follows the type (expense is stored negative). */
    @Positive(message = "Amount must be greater than zero")
    private BigDecimal amount;

    /** ISO-8601, same rules as on create (with offset exact, without offset = member's zone). */
    private String transactionDate;

    @Size(max = 255, message = "Description must not exceed 255 characters")
    private String transactionDesc;
}
