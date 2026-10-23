package com.vyra.be_virtual_your_assets.dto.transaction;

import com.vyra.be_virtual_your_assets.constant.transaction.PaymentMethod;
import com.vyra.be_virtual_your_assets.constant.transaction.TransactionCategory;
import com.vyra.be_virtual_your_assets.constant.transaction.TransactionType;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class CreateTransactionRequest {
    /** Legacy built-in category; ignored when {@link #categoryId} is set. */
    private TransactionCategory category;

    /** Optional. */
    private PaymentMethod paymentMethod;

    /** finance_category id (built-in or the member's own); takes precedence over {@link #category}. */
    private String categoryId;

    @NotNull(message = "Type is required")
    private TransactionType type;
    @NotNull(message = "Amount is required")
    @Positive(message = "Amount must be greater than zero")
    private BigDecimal amount;
    /**
     * ISO-8601 date-time of the transaction. With an offset ({@code 2026-10-22T08:30:00+07:00} or {@code ...Z})
     * it is exact; without one (older app versions) it is read in the member's time zone. Empty means now.
     */
    private String transactionDate;
    private String transactionDesc;
}
